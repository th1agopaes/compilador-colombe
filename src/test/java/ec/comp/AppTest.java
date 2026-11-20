package ec.comp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.Before;
import org.junit.Test;

/**
 * Testes da linha de comando: confere o código de saída e o que é impresso,
 * sem abrir console de verdade (a entrada e as saídas são buffers em memória).
 */
public class AppTest {

    private ByteArrayOutputStream saida;
    private ByteArrayOutputStream saidaErro;

    @Before
    public void prepararBuffers() {
        saida = new ByteArrayOutputStream();
        saidaErro = new ByteArrayOutputStream();
    }

    /** Executa o App com os argumentos e o que o "usuário digitaria" no teclado. */
    private int executar(String teclado, String... args) {
        App app = new App(
                new ByteArrayInputStream(teclado.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(saida, true, StandardCharsets.UTF_8),
                new PrintStream(saidaErro, true, StandardCharsets.UTF_8));
        return app.executar(args);
    }

    private String out() {
        return saida.toString(StandardCharsets.UTF_8);
    }

    private String err() {
        return saidaErro.toString(StandardCharsets.UTF_8);
    }

    @Test
    public void arquivoPelaLinhaDeComandoSemErros() {
        assertEquals(App.SAIDA_OK, executar("", "exemplos/registro-alunos.cb"));
        assertTrue(out().contains("Análise léxica concluída sem erros."));
        assertTrue(out().contains("Análise sintática concluída sem erros."));
    }

    @Test
    public void arquivoComErroSintaticoRetorna3() {
        assertEquals(App.SAIDA_ERRO_SINTATICO, executar("", "exemplos/erro-sintatico.cb"));
        assertTrue(out().contains("Erro sintático [linha 7, coluna 1]: esperado 'fimse', encontrado 'fim'"));
    }

    @Test
    public void arquivoComErrosLexicosRetorna1() {
        assertEquals(App.SAIDA_ERRO_LEXICO, executar("", "exemplos/erros-lexicos.cb"));
        assertTrue(out().contains("erro(s) léxico(s) encontrado(s)"));
    }

    @Test
    public void arquivoInexistenteRetorna2() {
        assertEquals(App.SAIDA_ERRO_USO, executar("", "nao-existe.cb"));
        assertTrue(err().contains("Arquivo não encontrado"));
    }

    @Test
    public void maisDeUmArgumentoRetorna2() {
        assertEquals(App.SAIDA_ERRO_USO, executar("", "a.cb", "b.cb"));
    }

    @Test
    public void semArgumentoPerguntaONomeDoArquivo() {
        // 1ª tentativa errada, 2ª certa: o programa deve insistir até receber um válido
        int codigo = executar("nao-existe.cb\nexemplos/todos-os-tokens.cb\n");
        assertEquals(App.SAIDA_OK, codigo);
        assertTrue(out().contains("Informe o nome do arquivo"));
        assertTrue(err().contains("Arquivo não encontrado"));
    }

    @Test
    public void caminhoEntreAspasDoWindows() {
        assertEquals(App.SAIDA_OK, executar("", "\"exemplos/registro-alunos.cb\""));
    }

    @Test
    public void usuarioDigitaSair() {
        assertEquals(App.SAIDA_ERRO_USO, executar("sair\n"));
    }

    @Test
    public void ajuda() {
        assertEquals(App.SAIDA_OK, executar("", "--help"));
        assertTrue(out().contains("Uso:"));
    }
}
