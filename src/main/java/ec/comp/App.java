package ec.comp;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Scanner;

import ec.comp.analisador.AnalisadorLexico;
import ec.comp.model.ErroLexico;
import ec.comp.model.TipoToken;
import ec.comp.model.Token;

/**
 * Programa principal do analisador léxico da Colombe.
 *
 * Duas formas de informar o arquivo:
 *   1. Linha de comando:   java -jar colombe.jar programa.cb
 *   2. Sem argumento:      java -jar colombe.jar
 *                          -> o programa pergunta o nome do arquivo.
 *
 * Código de saída: 0 = sem erros léxicos | 1 = com erros léxicos | 2 = erro de uso/arquivo.
 */
public final class App {

    static final int SAIDA_OK = 0;
    static final int SAIDA_ERRO_LEXICO = 1;
    static final int SAIDA_ERRO_USO = 2;

    // Para onde o programa escreve. No main são o console; nos testes,
    // são trocados por buffers em memória para conferir a saída.
    private final InputStream in;
    private final PrintStream out;
    private final PrintStream err;

    App(InputStream in, PrintStream out, PrintStream err) {
        this.in = in;
        this.out = out;
        this.err = err;
    }

    public static void main(String[] args) {
        int codigo = new App(System.in, System.out, System.err).executar(args);
        System.exit(codigo);
    }

    /** Toda a lógica do programa. Retorna o código de saída (em vez de chamar System.exit). */
    int executar(String[] args) {
        if (args.length > 0 && (args[0].equals("-h") || args[0].equals("--help"))) {
            mostrarUso();
            return SAIDA_OK;
        }
        if (args.length > 1) {
            err.println("Informe apenas UM arquivo.");
            mostrarUso();
            return SAIDA_ERRO_USO;
        }

        // args[0] existe? usa a linha de comando. Senão, pergunta ao usuário.
        Path arquivo = (args.length == 1) ? validarArquivo(args[0]) : solicitarArquivo();
        if (arquivo == null) {
            return SAIDA_ERRO_USO;
        }

        String codigo;
        try {
            codigo = lerArquivo(arquivo);
        } catch (IOException e) {
            err.println("Não foi possível ler o arquivo: " + e.getMessage());
            return SAIDA_ERRO_USO;
        }

        return analisar(arquivo, codigo) ? SAIDA_OK : SAIDA_ERRO_LEXICO;
    }

    /** Executa o léxico e imprime tokens, resumo e erros. Retorna true se não houve erro. */
    private boolean analisar(Path arquivo, String codigo) {
        AnalisadorLexico lexico = new AnalisadorLexico(codigo);
        Map<TipoToken, Integer> contagem = new EnumMap<>(TipoToken.class);

        out.println("==============================================================");
        out.println(" Analisador Léxico - Linguagem Colombe");
        out.println(" Arquivo: " + arquivo);
        out.println("==============================================================");
        out.println(" LIN:COL   TOKEN");

        // Mesmo laço do exemplo do professor: pede tokens até chegar no FIM
        Token token = lexico.nextToken();
        while (token.getTipoToken() != TipoToken.FIM) {
            out.printf("%4d:%-4d  %s%n", token.getLinha(), token.getColuna(), token);
            contagem.merge(token.getTipoToken(), 1, Integer::sum);
            token = lexico.nextToken();
        }

        int total = contagem.values().stream().mapToInt(Integer::intValue).sum();
        out.println("--------------------------------------------------------------");
        out.println(" Resumo: " + total + " tokens");
        for (Map.Entry<TipoToken, Integer> e : contagem.entrySet()) {
            out.printf("   %-18s %5d%n", e.getKey(), e.getValue());
        }
        out.println("--------------------------------------------------------------");

        if (!lexico.temErros()) {
            out.println(" Análise léxica concluída sem erros.");
            return true;
        }

        out.println(" " + lexico.getErros().size() + " erro(s) léxico(s) encontrado(s):");
        for (ErroLexico erro : lexico.getErros()) {
            out.println("   " + erro);
        }
        return false;
    }

    /** Pergunta o nome do arquivo até receber um válido (ou o usuário digitar "sair"). */
    private Path solicitarArquivo() {
        Scanner teclado = new Scanner(in, Charset.defaultCharset());
        while (true) {
            out.print("Informe o nome do arquivo a ser analisado (ou 'sair'): ");
            out.flush();
            if (!teclado.hasNextLine()) {
                return null; // entrada fechada (Ctrl+Z / Ctrl+D)
            }
            String nome = teclado.nextLine().trim();
            if (nome.equalsIgnoreCase("sair")) {
                return null;
            }
            if (nome.isEmpty()) {
                continue;
            }
            Path arquivo = validarArquivo(nome);
            if (arquivo != null) {
                return arquivo;
            }
        }
    }

    /** Confere se o caminho existe e é um arquivo legível. Retorna null se não for. */
    private Path validarArquivo(String nome) {
        // "Copiar como caminho" do Windows coloca aspas: "C:\...\prog.cb"
        if (nome.length() >= 2 && nome.startsWith("\"") && nome.endsWith("\"")) {
            nome = nome.substring(1, nome.length() - 1);
        }
        try {
            Path arquivo = Path.of(nome);
            if (!Files.exists(arquivo)) {
                err.println("Arquivo não encontrado: " + arquivo.toAbsolutePath());
                return null;
            }
            if (!Files.isRegularFile(arquivo) || !Files.isReadable(arquivo)) {
                err.println("Não é um arquivo legível: " + arquivo.toAbsolutePath());
                return null;
            }
            if (!nome.toLowerCase().endsWith(".cb")) {
                err.println("Aviso: arquivos Colombe costumam ter extensão .cb");
            }
            return arquivo;
        } catch (InvalidPathException e) {
            err.println("Nome de arquivo inválido: " + nome);
            return null;
        }
    }

    /**
     * Lê o arquivo como UTF-8. Se não for UTF-8 válido (arquivo salvo como
     * "ANSI" no Windows), tenta Windows-1252 para não estragar os acentos.
     */
    private String lerArquivo(Path arquivo) throws IOException {
        byte[] bytes = Files.readAllBytes(arquivo);
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException e) {
            err.println("Aviso: o arquivo não está em UTF-8; lendo como Windows-1252.");
            return new String(bytes, Charset.forName("windows-1252"));
        }
    }

    private void mostrarUso() {
        out.println("Uso:");
        out.println("  java -jar colombe.jar <arquivo.cb>");
        out.println("  java -jar colombe.jar              (pergunta o nome do arquivo)");
    }
}
