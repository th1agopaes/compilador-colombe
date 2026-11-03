package ec.comp.model;

/**
 * Unidade entregue pelo analisador léxico ao analisador sintático.
 * Guarda o tipo, o lexema (texto exato do código-fonte) e a posição
 * onde o token começa (linha e coluna, contadas a partir de 1).
 *
 * É IMUTÁVEL (campos final, sem setters): depois de criado, um token nunca
 * muda. Assim o sintático pode guardá-lo sem medo de alguém alterá-lo.
 */
public class Token {
    private final TipoToken tipoToken;
    private final String token;   // lexema
    private final int linha;
    private final int coluna;

    public Token(TipoToken tipoToken, String token, int linha, int coluna) {
        this.tipoToken = tipoToken;
        this.token = token;
        this.linha = linha;
        this.coluna = coluna;
    }

    public TipoToken getTipoToken() {
        return tipoToken;
    }

    public String getToken() {
        return token;
    }

    public int getLinha() {
        return linha;
    }

    public int getColuna() {
        return coluna;
    }

    /** Mesmo formato do exemplo do professor: <TIPO, "lexema"> */
    @Override
    public String toString() {
        return "<" + tipoToken + ", \"" + token + "\">";
    }
}
