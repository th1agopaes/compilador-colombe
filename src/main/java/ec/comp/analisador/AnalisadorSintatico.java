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

    /** { declaracao_global } */
    private void parseDeclaracoesGlobais() {
        // TODO módulo 2: const, var, registro e modulo
    }

    /** bloco = { declaracao_variavel } , { comando } ; */
    private void parseBloco() {
        // TODO módulos 2 a 4: variáveis locais e comandos
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

    /** O token atual é deste tipo E tem este lexema? Só olha, não avança. */
    private boolean verificar(TipoToken tipo, String lexema) {
        return verificar(tipo) && tokenAtual().getToken().equals(lexema);
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
