package ec.comp.model;

/**
 * Registro de um erro léxico: o que aconteceu e onde (linha:coluna).
 * O analisador NÃO para no primeiro erro: ele anota aqui e continua,
 * para mostrar todos os problemas do arquivo de uma vez.
 */
public class ErroLexico {
    private final String mensagem;
    private final int linha;
    private final int coluna;

    public ErroLexico(String mensagem, int linha, int coluna) {
        this.mensagem = mensagem;
        this.linha = linha;
        this.coluna = coluna;
    }

    public String getMensagem() {
        return mensagem;
    }

    public int getLinha() {
        return linha;
    }

    public int getColuna() {
        return coluna;
    }

    @Override
    public String toString() {
        return "Erro léxico [linha " + linha + ", coluna " + coluna + "]: " + mensagem;
    }
}
