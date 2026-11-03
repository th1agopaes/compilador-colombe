package ec.comp.analisador;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ec.comp.model.ErroLexico;
import ec.comp.model.TipoToken;
import ec.comp.model.Token;
import ec.comp.util.AlphaNumericoUtil;

/**
 * Analisador léxico (scanner) da linguagem Colombe.
 *
 * Ideia geral: o código-fonte é uma String e "pos" é um dedo apontando para o
 * caractere atual. A cada chamada de nextToken() o analisador:
 *   1. pula o que não vira token (espaços, tabulações, comentários);
 *   2. olha o caractere atual e decide qual "autômato" seguir
 *      (letra -> identificador, dígito -> número, aspas -> cadeia...);
 *   3. avança enquanto os caracteres servirem para aquele token
 *      (regra do maior casamento / maximal munch);
 *   4. devolve o Token com tipo, lexema, linha e coluna.
 *
 * Regras seguidas (especificação EBNF da Colombe):
 *   - case-sensitive;
 *   - quebra de linha vira o token NL, e quebras seguidas (linhas em branco
 *     ou só com comentário) são agrupadas em um único NL;
 *   - comentários // e /* *&#47; são descartados (bloco não aninha);
 *   - o sinal de negativo NÃO faz parte do número (-5 = operador "-" + 5).
 *
 * Erros léxicos não interrompem a análise: são registrados em getErros() e um
 * token do tipo ERRO é devolvido, para que o restante do arquivo seja analisado.
 */
public class AnalisadorLexico {

    private static final char FIM_DE_ENTRADA = '\0';

    private final String entrada;
    private final int tamanho;
    private int pos;

    // posição "humana" do caractere atual (começa em 1, como nos editores)
    private int linha = 1;
    private int coluna = 1;

    // true se o último token devolvido foi NL -> usado para agrupar quebras seguidas
    private boolean ultimoFoiNL = false;

    private final List<ErroLexico> erros = new ArrayList<>();

    public AnalisadorLexico(String entrada) {
        // Alguns editores do Windows gravam um BOM (U+FEFF) invisível no início
        // de arquivos UTF-8. Ele não é código, então é descartado.
        if (entrada.startsWith("﻿")) {
            entrada = entrada.substring(1);
        }
        this.entrada = entrada;
        this.tamanho = entrada.length();
        this.pos = 0;
    }

    // =====================================================================
    //  API pública
    // =====================================================================

    /** Retorna o próximo token do código-fonte (TipoToken.FIM quando acabar). */
    public Token nextToken() {
        while (true) {
            if (fimDaEntrada()) {
                return new Token(TipoToken.FIM, "", linha, coluna);
            }

            char ch = atual();

            // Espaço, tabulação e form feed: separam tokens e são descartados
            if (ch == ' ' || ch == '\t' || ch == '\f') {
                avancar();
                continue;
            }

            // Quebra de linha: terminador de instrução (token NL)
            if (ch == '\n' || ch == '\r') {
                int l = linha, c = coluna;
                consumirQuebraDeLinha();
                if (!ultimoFoiNL) {
                    ultimoFoiNL = true;
                    return new Token(TipoToken.NL, "\\n", l, c);
                }
                continue; // quebra repetida: absorvida no NL anterior
            }

            // Comentário de linha: // ... (a quebra de linha NÃO é consumida aqui,
            // assim ela ainda gera o NL que termina a instrução)
            if (ch == '/' && espiar(1) == '/') {
                pularComentarioDeLinha();
                continue;
            }

            // Comentário de bloco: /* ... */
            if (ch == '/' && espiar(1) == '*') {
                int l = linha, c = coluna;
                boolean atravessouLinha = pularComentarioDeBloco(l, c);
                // Um comentário de bloco que ocupa várias linhas "contém" uma
                // quebra de linha; sem isso, "x = 1 /*\n*/ y = 2" juntaria as
                // duas instruções em uma só.
                if (atravessouLinha && !ultimoFoiNL) {
                    ultimoFoiNL = true;
                    return new Token(TipoToken.NL, "\\n", l, c);
                }
                continue;
            }

            break; // chegou no início de um token de verdade
        }

        Token token = reconhecerToken();
        ultimoFoiNL = false;
        return token;
    }

    /** Analisa o arquivo inteiro e devolve todos os tokens (o último é FIM). */
    public List<Token> analisarTudo() {
        List<Token> tokens = new ArrayList<>();
        Token token;
        do {
            token = nextToken();
            tokens.add(token);
        } while (token.getTipoToken() != TipoToken.FIM);
        return tokens;
    }

    public List<ErroLexico> getErros() {
        return Collections.unmodifiableList(erros);
    }

    public boolean temErros() {
        return !erros.isEmpty();
    }

    // =====================================================================
    //  Escolha do autômato a partir do primeiro caractere
    // =====================================================================

    private Token reconhecerToken() {
        int l = linha, c = coluna;
        int inicio = pos;
        char ch = atual();

        // identificador = ( letra | "_" ) , { letra | digito | "_" }
        // (correção do exemplo original: ele testava isAlphaNumerico aqui, então
        //  "10" virava IDENTIFICADOR e o ramo de números nunca era executado)
        if (AlphaNumericoUtil.isInicioIdentificador(ch)) {
            return reconhecerPalavra(inicio, l, c);
        }

        if (AlphaNumericoUtil.isDigito(ch)) {
            return reconhecerNumero(inicio, l, c);
        }

        if (ch == '\'') {
            return reconhecerCaractere(inicio, l, c);
        }

        if (ch == '"') {
            return reconhecerCadeia(inicio, l, c);
        }

        switch (ch) {
            // Operadores que podem ter 2 caracteres (maior casamento:
            // "++" vence "+", "<=" vence "<"...)
            case '+': return operador(inicio, l, c, '+');   // +  ou ++
            case '-': return operador(inicio, l, c, '-');   // -  ou --
            case '=': return operador(inicio, l, c, '=');   // =  ou ==
            case '<': return operador(inicio, l, c, '=');   // <  ou <=
            case '>': return operador(inicio, l, c, '=');   // >  ou >=

            // Operadores de 1 caractere ("/" de comentário já foi tratado antes)
            case '*':
            case '/':
            case '^':
                avancar();
                return new Token(TipoToken.OPERADOR, String.valueOf(ch), l, c);

            // "!" só existe dentro de "!="
            case '!':
                avancar();
                if (atual() == '=') {
                    avancar();
                    return new Token(TipoToken.OPERADOR, "!=", l, c);
                }
                return erro("'!' sozinho não existe na Colombe: use 'nao' para negação ou '!=' para diferente",
                        inicio, l, c);

            // Delimitadores
            case '(':
            case ')':
            case '[':
            case ']':
            case ',':
            case ':':
            case '.':
                avancar();
                return new Token(TipoToken.DELIMITADOR, String.valueOf(ch), l, c);

            default:
                return reconhecerInvalido(inicio, l, c);
        }
    }

    // =====================================================================
    //  Autômatos de cada tipo de token
    // =====================================================================

    /** Identificador ou palavra reservada. */
    private Token reconhecerPalavra(int inicio, int l, int c) {
        while (!fimDaEntrada() && AlphaNumericoUtil.isParteIdentificador(atual())) {
            avancar();
        }

        // Ex.: "média" -> parou no 'é'. Em vez de gerar "m", ERRO, "dia",
        // consome a palavra inteira e reporta um único erro mais claro.
        if (!fimDaEntrada() && Character.isLetter(atual())) {
            consumirRestoDaPalavra();
            return erro("identificador '" + entrada.substring(inicio, pos)
                    + "' tem caractere não permitido (use só letras sem acento, dígitos e '_')", inicio, l, c);
        }

        String lexema = entrada.substring(inicio, pos);
        // Casou com identificador E com palavra reservada? Vale a palavra reservada.
        if (PalavrasReservadas.isPalavraReservada(lexema)) {
            return new Token(TipoToken.PALAVRA_RESERVADA, lexema, l, c);
        }
        return new Token(TipoToken.IDENTIFICADOR, lexema, l, c);
    }

    /**
     * literal_inteiro = digito , { digito }
     * literal_real    = digito , { digito } , "." , digito , { digito }
     */
    private Token reconhecerNumero(int inicio, int l, int c) {
        while (!fimDaEntrada() && AlphaNumericoUtil.isDigito(atual())) {
            avancar();
        }

        TipoToken tipo = TipoToken.LITERAL_INTEIRO;

        // Só é real se depois do "." vier um dígito (precisa olhar 2 à frente).
        // "3.14" -> real;  "v.x" e "3." não consomem o ponto aqui.
        if (atual() == '.' && AlphaNumericoUtil.isDigito(espiar(1))) {
            avancar(); // o "."
            while (!fimDaEntrada() && AlphaNumericoUtil.isDigito(atual())) {
                avancar();
            }
            tipo = TipoToken.LITERAL_REAL;
        }

        // "12abc": identificador não pode começar com dígito -> erro claro
        if (!fimDaEntrada() && (atual() == '_' || Character.isLetter(atual()))) {
            consumirRestoDaPalavra();
            return erro("número malformado '" + entrada.substring(inicio, pos)
                    + "' (identificadores não podem começar com dígito)", inicio, l, c);
        }

        return new Token(tipo, entrada.substring(inicio, pos), l, c);
    }

    /** literal_caractere = "'" , ( caractere_simples | escape ) , "'" */
    private Token reconhecerCaractere(int inicio, int l, int c) {
        avancar(); // abre '

        if (fimDaEntrada() || isQuebraDeLinha(atual())) {
            return erro("literal de caractere não terminado: falta o ' de fechamento", inicio, l, c);
        }

        if (atual() == '\'') {
            avancar();
            return erro("literal de caractere vazio ''", inicio, l, c);
        }

        boolean valido = true;
        if (atual() == '\\') {
            valido = lerEscape();
        } else {
            char ch = avancar();
            // Emoji e outros símbolos fora do BMP ocupam 2 chars em Java
            // (par substituto), mas são UM caractere para quem escreve: '😀'
            if (Character.isHighSurrogate(ch) && Character.isLowSurrogate(atual())) {
                avancar();
            }
        }

        if (atual() == '\'') {
            avancar(); // fecha '
            if (valido) {
                return new Token(TipoToken.LITERAL_CARACTERE, entrada.substring(inicio, pos), l, c);
            }
            return new Token(TipoToken.ERRO, entrada.substring(inicio, pos), l, c); // erro já registrado
        }

        // Não fechou logo depois de 1 caractere. Existe um ' mais adiante na mesma linha?
        int p = pos;
        while (p < tamanho && entrada.charAt(p) != '\'' && !isQuebraDeLinha(entrada.charAt(p))) {
            p++;
        }
        if (p < tamanho && entrada.charAt(p) == '\'') {
            while (pos <= p) {
                avancar(); // consome até o ' (recuperação de erro)
            }
            return erro("literal de caractere com mais de um caractere: para texto use cadeia (\"...\")",
                    inicio, l, c);
        }
        return erro("literal de caractere não terminado: falta o ' de fechamento", inicio, l, c);
    }

    /** literal_cadeia = '"' , { caractere_de_cadeia | escape } , '"' */
    private Token reconhecerCadeia(int inicio, int l, int c) {
        avancar(); // abre "
        boolean valido = true;

        while (true) {
            // cadeia não pode atravessar linhas
            if (fimDaEntrada() || isQuebraDeLinha(atual())) {
                return erro("cadeia não terminada: falta o \" de fechamento antes do fim da linha",
                        inicio, l, c);
            }
            char ch = atual();
            if (ch == '"') {
                avancar(); // fecha "
                break;
            }
            if (ch == '\\') {
                if (!lerEscape()) {
                    valido = false; // continua lendo para achar o fim da cadeia
                }
                continue;
            }
            avancar();
        }

        TipoToken tipo = valido ? TipoToken.LITERAL_CADEIA : TipoToken.ERRO;
        return new Token(tipo, entrada.substring(inicio, pos), l, c);
    }

    /**
     * escape = "\" , ( "n" | "t" | '"' | "'" | "\" )
     * Consome a barra e o caractere seguinte. Retorna false (e registra erro) se inválido.
     */
    private boolean lerEscape() {
        int l = linha, c = coluna;
        avancar(); // a barra
        if (fimDaEntrada() || isQuebraDeLinha(atual())) {
            registrarErro("sequência de escape incompleta: '\\' no fim da linha", l, c);
            return false;
        }
        char e = avancar();
        if ("nt\"'\\".indexOf(e) < 0) {
            registrarErro("sequência de escape inválida '\\" + e + "' (válidas: \\n \\t \\\" \\' \\\\)", l, c);
            return false;
        }
        return true;
    }

    /** Operador de 1 ou 2 caracteres: o segundo caractere só entra se for o esperado. */
    private Token operador(int inicio, int l, int c, char segundoEsperado) {
        avancar();
        if (atual() == segundoEsperado) {
            avancar();
        }
        return new Token(TipoToken.OPERADOR, entrada.substring(inicio, pos), l, c);
    }

    /** Caractere que não inicia nenhum token da Colombe. */
    private Token reconhecerInvalido(int inicio, int l, int c) {
        char ch = atual();

        // Palavra que começa com letra acentuada (ex.: "ação")
        if (Character.isLetter(ch)) {
            consumirRestoDaPalavra();
            return erro("identificador '" + entrada.substring(inicio, pos)
                    + "' tem caractere não permitido (use só letras sem acento, dígitos e '_')", inicio, l, c);
        }

        avancar();
        if (Character.isHighSurrogate(ch) && !fimDaEntrada() && Character.isLowSurrogate(atual())) {
            avancar(); // emoji e outros caracteres que ocupam 2 chars em Java
        }
        String lexema = entrada.substring(inicio, pos);
        return erro("caractere inválido " + descrever(lexema) + dicaPara(ch), inicio, l, c);
    }

    /** Dicas para erros comuns de quem vem de C/Java. */
    private String dicaPara(char ch) {
        switch (ch) {
            case ';': return " (a Colombe não usa ';': a instrução termina na quebra de linha)";
            case '{':
            case '}': return " (a Colombe não usa chaves: blocos fecham com fim, fimse, fimenquanto, fimpara...)";
            case '&': return " (para E lógico use a palavra 'e')";
            case '|': return " (para OU lógico use a palavra 'ou')";
            case '%': return " (para resto da divisão use 'mod')";
            default:  return "";
        }
    }

    // =====================================================================
    //  Elementos descartados
    // =====================================================================

    private void pularComentarioDeLinha() {
        while (!fimDaEntrada() && !isQuebraDeLinha(atual())) {
            avancar();
        }
    }

    /** Pula /* ... *&#47;. Retorna true se o comentário atravessou alguma linha. */
    private boolean pularComentarioDeBloco(int l, int c) {
        avancar(); // '/'
        avancar(); // '*'
        boolean atravessouLinha = false;

        while (!fimDaEntrada()) {
            if (atual() == '*' && espiar(1) == '/') {
                avancar();
                avancar();
                return atravessouLinha;
            }
            if (isQuebraDeLinha(avancar())) {
                atravessouLinha = true;
            }
        }
        registrarErro("comentário de bloco não terminado: falta '*/'", l, c);
        return atravessouLinha;
    }

    // =====================================================================
    //  Navegação na entrada (controle de pos, linha e coluna)
    // =====================================================================

    private boolean fimDaEntrada() {
        return pos >= tamanho;
    }

    /** Caractere atual, sem avançar. */
    private char atual() {
        return espiar(0);
    }

    /** "Espia" k posições à frente sem avançar (lookahead). */
    private char espiar(int k) {
        int p = pos + k;
        return p < tamanho ? entrada.charAt(p) : FIM_DE_ENTRADA;
    }

    /** Consome o caractere atual e atualiza linha/coluna. */
    private char avancar() {
        char ch = entrada.charAt(pos++);
        if (ch == '\n') {
            linha++;
            coluna = 1;
        } else if (ch == '\r') {
            // Windows usa \r\n: deixa o \n seguinte contar a linha.
            // \r sozinho (Mac antigo) conta como quebra.
            if (atual() != '\n') {
                linha++;
                coluna = 1;
            }
        } else if (!Character.isHighSurrogate(ch)) {
            coluna++;
        }
        return ch;
    }

    /** Consome \n, \r\n ou \r como UMA quebra de linha. */
    private void consumirQuebraDeLinha() {
        char ch = avancar();
        if (ch == '\r' && atual() == '\n') {
            avancar();
        }
    }

    private static boolean isQuebraDeLinha(char ch) {
        return ch == '\n' || ch == '\r';
    }

    /** Recuperação de erro: engole o resto de uma "palavra" malformada. */
    private void consumirRestoDaPalavra() {
        while (!fimDaEntrada() && (Character.isLetterOrDigit(atual()) || atual() == '_')) {
            avancar();
        }
    }

    // =====================================================================
    //  Erros
    // =====================================================================

    private void registrarErro(String mensagem, int l, int c) {
        erros.add(new ErroLexico(mensagem, l, c));
    }

    private Token erro(String mensagem, int inicio, int l, int c) {
        registrarErro(mensagem, l, c);
        return new Token(TipoToken.ERRO, entrada.substring(inicio, pos), l, c);
    }

    /** Mostra caracteres invisíveis como U+XXXX para a mensagem ficar legível. */
    private static String descrever(String s) {
        int cp = s.codePointAt(0);
        if (Character.isISOControl(cp) || Character.isWhitespace(cp) || Character.getType(cp) == Character.FORMAT) {
            return String.format("U+%04X", cp);
        }
        return "'" + s + "'";
    }
}
