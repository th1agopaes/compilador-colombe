package ec.comp.analisador;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import ec.comp.model.ErroSintatico;

/**
 * Testes do analisador sintático: programas válidos são aceitos e programas
 * com erro são rejeitados na linha e coluna certas, com a mensagem certa.
 */
public class AnalisadorSintaticoTest {

    // ---------- auxiliares ----------

    /** Roda léxico + sintático; lança ErroSintatico se o código for inválido. */
    private static void analisar(String codigo) {
        new AnalisadorSintatico(new AnalisadorLexico(codigo).analisarTudo()).parsePrograma();
    }

    /** Garante que o código é rejeitado e devolve o erro para conferir detalhes. */
    private static ErroSintatico erro(String codigo) {
        return assertThrows(ErroSintatico.class, () -> analisar(codigo));
    }

    // ---------- programa ----------

    @Test
    public void aceitaProgramaMinimo() {
        analisar("programa Ola\ninicio\nfim\n");
    }

    @Test
    public void aceitaComentarioNoInicioESemQuebraNoFinal() {
        analisar("// meu primeiro programa\nprograma Ola\ninicio\nfim");
    }

    @Test
    public void rejeitaProgramaSemInicio() {
        ErroSintatico e = erro("programa Ola\nfim\n");
        assertEquals("esperado 'inicio', encontrado 'fim'", e.getMessage());
        assertEquals(2, e.getLinha());
        assertEquals(1, e.getColuna());
    }

    @Test
    public void rejeitaProgramaSemNome() {
        ErroSintatico e = erro("programa\ninicio\nfim\n");
        assertEquals("esperado um identificador, encontrado fim de linha", e.getMessage());
        assertEquals(1, e.getLinha());
    }

    @Test
    public void rejeitaCodigoDepoisDoFim() {
        ErroSintatico e = erro("programa Ola\ninicio\nfim\nx = 1\n");
        assertEquals("esperado fim do arquivo, encontrado 'x'", e.getMessage());
        assertEquals(4, e.getLinha());
    }
}
