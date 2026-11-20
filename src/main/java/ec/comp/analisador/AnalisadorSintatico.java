package ec.comp.analisador;

import java.util.List;

import ec.comp.model.ErroSintatico;
import ec.comp.model.TipoToken;
import ec.comp.model.Token;

/**
 * Analisador sintático (parser) da linguagem Colombe.
 *
 * Técnica: descendente recursivo preditivo. Cada regra (não-terminal) da EBNF
 * vira um método parseXxx(), que lê a regra da esquerda para a direita:
 *   - terminal            -> consumir(...)
 *   - { ... } (repetição) -> while
 *   - [ ... ] (opcional)  -> if
 *   - a | b (alternativa) -> if / else, decidido pelo token atual (lookahead)
 *
 * A gramática da Colombe é LL(1): olhar UM token à frente sempre basta para
 * escolher a regra, então o parser nunca precisa voltar atrás (backtracking).
 *
 * No primeiro erro, lança ErroSintatico com a linha e a coluna do token.
 */
public class AnalisadorSintatico {

    private static final String[] TIPOS_PRIMITIVOS = {"int", "real", "logico", "caractere", "cadeia"};
    private final List<Token> tokens;
    private int pos = 0;

    public AnalisadorSintatico(List<Token> tokens) {
        this.tokens = tokens;
    }

    // =====================================================================
    //  Regras da gramática
    // =====================================================================

    /**
     * programa = [ NL ] , "programa" , identificador , NL ,
     *            { declaracao_global } ,
     *            "inicio" , NL , bloco , "fim" , [ NL ] ;
     */
    public void parsePrograma() {
        if (verificar(TipoToken.NL)) {          // [ NL ]
            consumir(TipoToken.NL);
        }
        consumir(TipoToken.PALAVRA_RESERVADA, "programa");
        consumir(TipoToken.IDENTIFICADOR);
        consumir(TipoToken.NL);
        parseDeclaracoesGlobais();              // { declaracao_global }
        consumir(TipoToken.PALAVRA_RESERVADA, "inicio");
        consumir(TipoToken.NL);
        parseBloco();
        consumir(TipoToken.PALAVRA_RESERVADA, "fim");
        if (verificar(TipoToken.NL)) {          // [ NL ]
            consumir(TipoToken.NL);
        }
        consumir(TipoToken.FIM);                // nada depois do "fim"
    }

    /**
     * { declaracao_global }
     * Repete enquanto o token atual puder COMEÇAR uma declaração global.
     */
    private void parseDeclaracoesGlobais() {
        // TODO: incluir "modulo" quando a regra declaracao_modulo existir
        while (verificar(TipoToken.PALAVRA_RESERVADA, "const", "var", "registro")) {
            parseDeclaracaoGlobal();
        }
    }

    /** declaracao_global = declaracao_constante | declaracao_variavel | declaracao_registro | ... ; */
    private void parseDeclaracaoGlobal() {
        if (verificar(TipoToken.PALAVRA_RESERVADA, "const")) {
            parseDeclaracaoConstante();
        } else if (verificar(TipoToken.PALAVRA_RESERVADA, "var")) {
            parseDeclaracaoVariavel();
        } else {
            parseDeclaracaoRegistro();
        }
    }

    /** bloco = { declaracao_variavel } , { comando } ; */
    private void parseBloco() {
        while (verificar(TipoToken.PALAVRA_RESERVADA, "var")) {
            parseDeclaracaoVariavel();
        }
        // TODO: { comando }
    }

    // ---------- declarações de dados ----------

    /** declaracao_constante = "const" , identificador , ":" , tipo_primitivo , "=" , expressao , NL ; */
    private void parseDeclaracaoConstante() {
        consumir(TipoToken.PALAVRA_RESERVADA, "const");
        consumir(TipoToken.IDENTIFICADOR);
        consumir(TipoToken.DELIMITADOR, ":");
        parseTipoPrimitivo();
        consumir(TipoToken.OPERADOR, "=");
        parseExpressao();
        consumir(TipoToken.NL);
    }

    /** declaracao_variavel = "var" , identificador , ":" , tipo , [ "=" , expressao ] , NL ; */
    private void parseDeclaracaoVariavel() {
        consumir(TipoToken.PALAVRA_RESERVADA, "var");
        consumir(TipoToken.IDENTIFICADOR);
        consumir(TipoToken.DELIMITADOR, ":");
        parseTipo();
        if (verificar(TipoToken.OPERADOR, "=")) {   // [ "=" , expressao ]
            consumir(TipoToken.OPERADOR, "=");
            parseExpressao();
        }
        consumir(TipoToken.NL);
    }

    /** declaracao_registro = "registro" , identificador , NL , campo , { campo } , "fim" , NL ; */
    private void parseDeclaracaoRegistro() {
        consumir(TipoToken.PALAVRA_RESERVADA, "registro");
        consumir(TipoToken.IDENTIFICADOR);
        consumir(TipoToken.NL);
        parseCampo();                               // campo   (pelo menos um)
        while (verificar(TipoToken.IDENTIFICADOR)) {
            parseCampo();                           // { campo }
        }
        consumir(TipoToken.PALAVRA_RESERVADA, "fim");
        consumir(TipoToken.NL);
    }

    /** campo = identificador , ":" , tipo , NL ; */
    private void parseCampo() {
        consumir(TipoToken.IDENTIFICADOR);
        consumir(TipoToken.DELIMITADOR, ":");
        parseTipo();
        consumir(TipoToken.NL);
    }

    // ---------- tipos ----------

    /** tipo = tipo_base | tipo_vetor | tipo_matriz ; */
    private void parseTipo() {
        if (verificar(TipoToken.PALAVRA_RESERVADA, "vetor")) {
            parseTipoVetor();
        } else if (verificar(TipoToken.PALAVRA_RESERVADA, "matriz")) {
            parseTipoMatriz();
        } else {
            parseTipoBase();
        }
    }

    /** tipo_base = tipo_primitivo | identificador ; (identificador = nome de registro) */
    private void parseTipoBase() {
        if (verificar(TipoToken.PALAVRA_RESERVADA, TIPOS_PRIMITIVOS)) {
            avancar();
        } else if (verificar(TipoToken.IDENTIFICADOR)) {
            avancar();
        } else {
            throw erro("um tipo (primitivo ou nome de registro)");
        }
    }

    /** tipo_primitivo = "int" | "real" | "logico" | "caractere" | "cadeia" ; */
    private void parseTipoPrimitivo() {
        if (!verificar(TipoToken.PALAVRA_RESERVADA, TIPOS_PRIMITIVOS)) {
            throw erro("um tipo primitivo");
        }
        avancar();
    }

    /** tipo_vetor = "vetor" , "[" , tamanho , "]" , "de" , tipo_base ; */
    private void parseTipoVetor() {
        consumir(TipoToken.PALAVRA_RESERVADA, "vetor");
        consumir(TipoToken.DELIMITADOR, "[");
        parseTamanho();
        consumir(TipoToken.DELIMITADOR, "]");
        consumir(TipoToken.PALAVRA_RESERVADA, "de");
        parseTipoBase();
    }

    /** tipo_matriz = "matriz" , "[" , tamanho , "]" , "[" , tamanho , "]" , "de" , tipo_base ; */
    private void parseTipoMatriz() {
        consumir(TipoToken.PALAVRA_RESERVADA, "matriz");
        consumir(TipoToken.DELIMITADOR, "[");
        parseTamanho();
        consumir(TipoToken.DELIMITADOR, "]");
        consumir(TipoToken.DELIMITADOR, "[");
        parseTamanho();
        consumir(TipoToken.DELIMITADOR, "]");
        consumir(TipoToken.PALAVRA_RESERVADA, "de");
        parseTipoBase();
    }

    /** tamanho = literal_inteiro | identificador ; (identificador = constante inteira) */
    private void parseTamanho() {
        if (verificar(TipoToken.LITERAL_INTEIRO) || verificar(TipoToken.IDENTIFICADOR)) {
            avancar();
        } else {
            throw erro("um tamanho (inteiro ou constante)");
        }
    }

    // ---------- variáveis e chamadas ----------

    /** variavel = identificador , { seletor } ; */
    private void parseVariavel() {
        consumir(TipoToken.IDENTIFICADOR);
        parseSeletores();
    }

    /** { seletor }  -- repete enquanto vier "[" ou "." */
    private void parseSeletores() {
        while (verificar(TipoToken.DELIMITADOR, "[", ".")) {
            parseSeletor();
        }
    }

    /** seletor = "[" , expressao , "]" | "." , identificador ; */
    private void parseSeletor() {
        if (verificar(TipoToken.DELIMITADOR, "[")) {   // índice: v[i]
            avancar();
            parseExpressao();
            consumir(TipoToken.DELIMITADOR, "]");
        } else {                                        // campo: aluno.nome
            consumir(TipoToken.DELIMITADOR, ".");
            consumir(TipoToken.IDENTIFICADOR);
        }
    }

    /** "(" , [ lista_argumentos ] , ")"  -- parte da chamada de módulo */
    private void parseArgumentos() {
        consumir(TipoToken.DELIMITADOR, "(");
        if (!verificar(TipoToken.DELIMITADOR, ")")) {   // [ lista_argumentos ]
            parseListaArgumentos();
        }
        consumir(TipoToken.DELIMITADOR, ")");
    }

    /** lista_argumentos = expressao , { "," , expressao } ; */
    private void parseListaArgumentos() {
        parseExpressao();
        while (verificar(TipoToken.DELIMITADOR, ",")) {
            avancar();
            parseExpressao();
        }
    }

    // ---------- expressões (nível 8 = menor precedência ... nível 1 = maior) ----------

    /** expressao = expr_e , { ( "ou" | "xou" ) , expr_e } ;   (nível 8) */
    private void parseExpressao() {
        parseExprE();
        while (verificar(TipoToken.PALAVRA_RESERVADA, "ou", "xou")) {
            avancar();
            parseExprE();
        }
    }

    /** expr_e = expr_igualdade , { "e" , expr_igualdade } ;   (nível 7) */
    private void parseExprE() {
        parseExprIgualdade();
        while (verificar(TipoToken.PALAVRA_RESERVADA, "e")) {
            avancar();
            parseExprIgualdade();
        }
    }

    /** expr_igualdade = expr_relacional , [ ( "==" | "!=" ) , expr_relacional ] ;   (nível 6) */
    private void parseExprIgualdade() {
        parseExprRelacional();
        if (verificar(TipoToken.OPERADOR, "==", "!=")) {      // [ ] -> if: não encadeia
            avancar();
            parseExprRelacional();
        }
    }

    /** expr_relacional = expr_aditiva , [ ( "<" | ">" | "<=" | ">=" ) , expr_aditiva ] ;   (nível 5) */
    private void parseExprRelacional() {
        parseExprAditiva();
        if (verificar(TipoToken.OPERADOR, "<", ">", "<=", ">=")) {
            avancar();
            parseExprAditiva();
        }
    }

    /** expr_aditiva = expr_multiplicativa , { ( "+" | "-" ) , expr_multiplicativa } ;   (nível 4) */
    private void parseExprAditiva() {
        parseExprMultiplicativa();
        while (verificar(TipoToken.OPERADOR, "+", "-")) {
            avancar();
            parseExprMultiplicativa();
        }
    }

    /** expr_multiplicativa = expr_potencia , { ( "*" | "/" | "div" | "mod" ) , expr_potencia } ;   (nível 3) */
    private void parseExprMultiplicativa() {
        parseExprPotencia();
        while (verificar(TipoToken.OPERADOR, "*", "/")
                || verificar(TipoToken.PALAVRA_RESERVADA, "div", "mod")) {
            avancar();
            parseExprPotencia();
        }
    }

    /** expr_potencia = expr_unaria , [ "^" , expr_potencia ] ;   (nível 2) */
    private void parseExprPotencia() {
        parseExprUnaria();
        if (verificar(TipoToken.OPERADOR, "^")) {
            avancar();
            parseExprPotencia();          // chama a si mesma: 2^3^2 = 2^(3^2)
        }
    }

    /**
     * expr_unaria = ( "nao" | "-" ) , expr_unaria
     *             | ( "++" | "--" ) , variavel
     *             | primario ;   (nível 1)
     */
    private void parseExprUnaria() {
        if (verificar(TipoToken.PALAVRA_RESERVADA, "nao") || verificar(TipoToken.OPERADOR, "-")) {
            avancar();
            parseExprUnaria();
        } else if (verificar(TipoToken.OPERADOR, "++", "--")) {
            avancar();
            parseVariavel();
        } else {
            parsePrimario();
        }
    }

    /**
     * primario = literal
     *          | "(" , expressao , ")"
     *          | identificador , ( "(" , [ lista_argumentos ] , ")"
     *                            | { seletor } , [ "++" | "--" ] ) ;
     */
    private void parsePrimario() {
        if (verificarLiteral()) {
            avancar();
        } else if (verificar(TipoToken.DELIMITADOR, "(")) {
            avancar();
            parseExpressao();
            consumir(TipoToken.DELIMITADOR, ")");
        } else if (verificar(TipoToken.IDENTIFICADOR)) {
            avancar();
            if (verificar(TipoToken.DELIMITADOR, "(")) {     // chamada: somar(a, b)
                parseArgumentos();
            } else {                                         // variável: turma[i].nome
                parseSeletores();
                if (verificar(TipoToken.OPERADOR, "++", "--")) {   // [ "++" | "--" ]
                    avancar();
                }
            }
        } else {
            throw erro("uma expressão");
        }
    }

    // =====================================================================
    //  Auxiliares
    // =====================================================================

    /** Token da posição atual; se passar do fim, devolve o último (FIM). */
    private Token tokenAtual() {
        if (pos >= tokens.size()) {
            return tokens.get(tokens.size() - 1);
        }
        return tokens.get(pos);
    }

    /** O token atual é deste tipo? Só olha, não avança. */
    private boolean verificar(TipoToken tipo) {
        return tokenAtual().getTipoToken() == tipo;
    }

    /** O token atual é deste tipo E tem UM destes lexemas? Só olha, não avança. */
    private boolean verificar(TipoToken tipo, String... lexemas) {
        if (!verificar(tipo)) {
            return false;
        }
        for (String lexema : lexemas) {
            if (tokenAtual().getToken().equals(lexema)) {
                return true;
            }
        }
        return false;
    }

    /** literal = literal_inteiro | literal_real | literal_logico | literal_caractere | literal_cadeia ; */
    private boolean verificarLiteral() {
        return verificar(TipoToken.LITERAL_INTEIRO) || verificar(TipoToken.LITERAL_REAL)
                || verificar(TipoToken.LITERAL_CARACTERE) || verificar(TipoToken.LITERAL_CADEIA)
                || verificar(TipoToken.PALAVRA_RESERVADA, "verdadeiro", "falso");
    }

    /** Exige um token deste tipo: se for, avança e o devolve; se não, erro. */
    private Token consumir(TipoToken tipo) {
        if (!verificar(tipo)) {
            throw erro(descrever(tipo));
        }
        return avancar();
    }

    /** Exige um token deste tipo com este lexema (ex.: a palavra "inicio"). */
    private Token consumir(TipoToken tipo, String lexema) {
        if (!verificar(tipo, lexema)) {
            throw erro("'" + lexema + "'");
        }
        return avancar();
    }

    private Token avancar() {
        Token atual = tokenAtual();
        pos++;
        return atual;
    }

    /** Monta o erro "esperado X, encontrado Y" na posição do token atual. */
    private ErroSintatico erro(String esperado) {
        Token t = tokenAtual();
        return new ErroSintatico("esperado " + esperado + ", encontrado " + descrever(t),
                t.getLinha(), t.getColuna());
    }

    /** Como um token aparece na mensagem de erro. */
    private static String descrever(Token t) {
        return switch (t.getTipoToken()) {
            case NL -> "fim de linha";
            case FIM -> "fim do arquivo";
            default -> "'" + t.getToken() + "'";
        };
    }

    /** Como um tipo de token esperado aparece na mensagem de erro. */
    private static String descrever(TipoToken tipo) {
        return switch (tipo) {
            case NL -> "fim de linha";
            case FIM -> "fim do arquivo";
            case IDENTIFICADOR -> "um identificador";
            default -> tipo.name().toLowerCase().replace('_', ' ');
        };
    }
}
