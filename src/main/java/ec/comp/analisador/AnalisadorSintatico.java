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
    /** Palavras reservadas que podem começar um comando (parte do FIRST(comando)) */
    private static final String[] PALAVRAS_DE_COMANDO = {"se", "enquanto", "repita", "para", "leia", "escreve", "retorna"};
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
        while (verificar(TipoToken.PALAVRA_RESERVADA, "const", "var", "registro", "modulo")) {
            parseDeclaracaoGlobal();
        }
    }

    /** declaracao_global = declaracao_constante | declaracao_variavel | declaracao_registro | declaracao_modulo ; */
    private void parseDeclaracaoGlobal() {
        if (verificar(TipoToken.PALAVRA_RESERVADA, "const")) {
            parseDeclaracaoConstante();
        } else if (verificar(TipoToken.PALAVRA_RESERVADA, "var")) {
            parseDeclaracaoVariavel();
        } else if (verificar(TipoToken.PALAVRA_RESERVADA, "registro")) {
            parseDeclaracaoRegistro();
        } else {
            parseDeclaracaoModulo();   // o while já garantiu que é "modulo"
        }
    }

    /** bloco = { declaracao_variavel } , { comando } ; */
    private void parseBloco() {
        while (verificar(TipoToken.PALAVRA_RESERVADA, "var")) {
            parseDeclaracaoVariavel();
        }
        parseComandos();
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

    /** tipo_retorno = tipo | "vazio" ; */
    private void parseTipoRetorno() {
        if (verificar(TipoToken.PALAVRA_RESERVADA, "vazio")) {
            avancar();
        } else {
            parseTipo();
        }
    }

    // ---------- módulos ----------

    /**
     * declaracao_modulo = "modulo" , identificador , "(" , [ lista_parametros ] , ")" ,
     *                     ":" , tipo_retorno , NL , bloco , "fim" , NL ;
     */
    private void parseDeclaracaoModulo() {
        consumir(TipoToken.PALAVRA_RESERVADA, "modulo");
        consumir(TipoToken.IDENTIFICADOR);
        consumir(TipoToken.DELIMITADOR, "(");
        if (!verificar(TipoToken.DELIMITADOR, ")")) {   // [ lista_parametros ]
            parseListaParametros();
        }
        consumir(TipoToken.DELIMITADOR, ")");
        consumir(TipoToken.DELIMITADOR, ":");
        parseTipoRetorno();
        consumir(TipoToken.NL);
        parseBloco();                                   // o mesmo bloco do inicio...fim
        consumir(TipoToken.PALAVRA_RESERVADA, "fim");
        consumir(TipoToken.NL);
    }

    /** lista_parametros = parametro , { "," , parametro } ; */
    private void parseListaParametros() {
        parseParametro();
        while (verificar(TipoToken.DELIMITADOR, ",")) {
            avancar();
            parseParametro();
        }
    }

    /** parametro = [ "ref" ] , identificador , ":" , tipo ; */
    private void parseParametro() {
        if (verificar(TipoToken.PALAVRA_RESERVADA, "ref")) {   // [ "ref" ] -> por referência
            avancar();
        }
        consumir(TipoToken.IDENTIFICADOR);
        consumir(TipoToken.DELIMITADOR, ":");
        parseTipo();
    }

    // ---------- comandos ----------

    /** { comando } -- repete enquanto o token atual puder COMEÇAR um comando */
    private void parseComandos() {
        while (podeIniciarComando()) {
            parseComando();
        }
    }

    /** FIRST(comando) = { identificador, "++", "--", se, enquanto, repita, para, leia, escreve, retorna } */
    private boolean podeIniciarComando() {
        return verificar(TipoToken.IDENTIFICADOR)
                || verificar(TipoToken.OPERADOR, "++", "--")
                || verificar(TipoToken.PALAVRA_RESERVADA, PALAVRAS_DE_COMANDO);
    }

    /**
     * comando = ( comando_identificador | comando_incremento | comando_se
     *         | comando_enquanto | comando_repita | comando_para
     *         | comando_leia | comando_escreve | comando_retorna ) , NL ;
     */
    private void parseComando() {
        if (verificar(TipoToken.IDENTIFICADOR)) {
            parseComandoIdentificador();
        } else if (verificar(TipoToken.OPERADOR, "++", "--")) {
            parseComandoIncremento();
        } else {
            switch (tokenAtual().getToken()) {
                case "leia" -> parseComandoLeia();
                case "escreve" -> parseComandoEscreve();
                case "retorna" -> parseComandoRetorna();
                case "se" -> parseComandoSe();
                case "enquanto" -> parseComandoEnquanto();
                case "repita" -> parseComandoRepita();
                case "para" -> parseComandoPara();
                default -> throw erro("um comando");
            }
        }
        consumir(TipoToken.NL);     // todo comando termina na quebra de linha
    }

    /**
     * comando_identificador = identificador ,
     *       ( "(" , [ lista_argumentos ] , ")"
     *       | { seletor } , ( "=" , expressao | "++" | "--" ) ) ;
     */
    private void parseComandoIdentificador() {
        consumir(TipoToken.IDENTIFICADOR);
        if (verificar(TipoToken.DELIMITADOR, "(")) {          // chamada: somar(a, b)
            parseArgumentos();
        } else {
            parseSeletores();                                  // turma[i].nome
            if (verificar(TipoToken.OPERADOR, "=")) {          // atribuição
                avancar();
                parseExpressao();
            } else if (verificar(TipoToken.OPERADOR, "++", "--")) {   // i++ / i--
                avancar();
            } else {
                throw erro("'=', '++' ou '--'");
            }
        }
    }

    /** comando_incremento = ( "++" | "--" ) , variavel ; */
    private void parseComandoIncremento() {
        avancar();                                 // "++" ou "--" (já verificado)
        parseVariavel();
    }

    /** comando_leia = "leia" , "(" , variavel , ")" ; */
    private void parseComandoLeia() {
        consumir(TipoToken.PALAVRA_RESERVADA, "leia");
        consumir(TipoToken.DELIMITADOR, "(");
        parseVariavel();
        consumir(TipoToken.DELIMITADOR, ")");
    }

    /** comando_escreve = "escreve" , "(" , expressao , { "," , expressao } , ")" ; */
    private void parseComandoEscreve() {
        consumir(TipoToken.PALAVRA_RESERVADA, "escreve");
        consumir(TipoToken.DELIMITADOR, "(");
        parseListaArgumentos();                    // mesma forma: expressao , { "," , expressao }
        consumir(TipoToken.DELIMITADOR, ")");
    }

    /** comando_retorna = "retorna" , expressao ; */
    private void parseComandoRetorna() {
        consumir(TipoToken.PALAVRA_RESERVADA, "retorna");
        parseExpressao();
    }

    /** comando_se = "se" , expressao , "entao" , NL , { comando } , [ "senao" , NL , { comando } ] , "fimse" ; */
    private void parseComandoSe() {
        consumir(TipoToken.PALAVRA_RESERVADA, "se");
        parseExpressao();
        consumir(TipoToken.PALAVRA_RESERVADA, "entao");
        consumir(TipoToken.NL);
        parseComandos();
        if (verificar(TipoToken.PALAVRA_RESERVADA, "senao")) {   // [ senao ... ]
            avancar();
            consumir(TipoToken.NL);
            parseComandos();
        }
        consumir(TipoToken.PALAVRA_RESERVADA, "fimse");
    }

    /** comando_enquanto = "enquanto" , expressao , "faca" , NL , { comando } , "fimenquanto" ; */
    private void parseComandoEnquanto() {
        consumir(TipoToken.PALAVRA_RESERVADA, "enquanto");
        parseExpressao();
        consumir(TipoToken.PALAVRA_RESERVADA, "faca");
        consumir(TipoToken.NL);
        parseComandos();
        consumir(TipoToken.PALAVRA_RESERVADA, "fimenquanto");
    }

    /** comando_repita = "repita" , NL , { comando } , "ate" , expressao ; */
    private void parseComandoRepita() {
        consumir(TipoToken.PALAVRA_RESERVADA, "repita");
        consumir(TipoToken.NL);
        parseComandos();
        consumir(TipoToken.PALAVRA_RESERVADA, "ate");
        parseExpressao();
    }

    /**
     * comando_para = "para" , identificador , "de" , expressao , "ate" , expressao ,
     *                [ "passo" , expressao ] , "faca" , NL , { comando } , "fimpara" ;
     */
    private void parseComandoPara() {
        consumir(TipoToken.PALAVRA_RESERVADA, "para");
        consumir(TipoToken.IDENTIFICADOR);
        consumir(TipoToken.PALAVRA_RESERVADA, "de");
        parseExpressao();
        consumir(TipoToken.PALAVRA_RESERVADA, "ate");
        parseExpressao();
        if (verificar(TipoToken.PALAVRA_RESERVADA, "passo")) {   // [ passo ... ]
            avancar();
            parseExpressao();
        }
        consumir(TipoToken.PALAVRA_RESERVADA, "faca");
        consumir(TipoToken.NL);
        parseComandos();
        consumir(TipoToken.PALAVRA_RESERVADA, "fimpara");
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
