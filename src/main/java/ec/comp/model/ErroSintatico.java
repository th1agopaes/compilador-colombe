package ec.comp.model;

/**
 * Erro sintático: o programa não segue a gramática da Colombe.
 *
 * Diferente do léxico (que anota os erros numa lista e continua), o sintático
 * PARA no primeiro erro, como no exemplo do professor. Por isso este erro é uma
 * exceção: é lançado no ponto do problema e capturado lá em cima, no App.
 */
public class ErroSintatico extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final int linha;
    private final int coluna;

    public ErroSintatico(String mensagem, int linha, int coluna) {
        super(mensagem);
        this.linha = linha;
        this.coluna = coluna;
    }

    public int getLinha() {
        return linha;
    }

    public int getColuna() {
        return coluna;
    }

    @Override
    public String toString() {
        return "Erro sintático [linha " + linha + ", coluna " + coluna + "]: " + getMessage();
    }
}
