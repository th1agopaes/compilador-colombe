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

    /** Monta um programa com estas declarações globais (antes do "inicio"). */
    private static String comGlobais(String declaracoes) {
        return "programa Teste\n" + declaracoes + "inicio\nfim\n";
    }

    /** Monta um programa com estas declarações locais (depois do "inicio"). */
    private static String comLocais(String declaracoes) {
        return "programa Teste\ninicio\n" + declaracoes + "fim\n";
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

    // ---------- declarações e tipos ----------

    @Test
    public void aceitaConstantes() {
        analisar(comGlobais("""
                const MAX: int = 30
                const PI: real = 3.14
                const NOME: cadeia = "Colombe"
                """));
    }

    @Test
    public void aceitaVariaveisDeTiposPrimitivosComESemValorInicial() {
        analisar(comGlobais("""
                var idade: int
                var media: real = 0.0
                var aprovado: logico = falso
                var letra: caractere = 'A'
                var nome: cadeia
                """));
    }

    @Test
    public void aceitaVetorEMatrizComTamanhoLiteralOuConstante() {
        analisar(comGlobais("""
                const N: int = 3
                var notas: vetor[5] de real
                var tabuleiro: matriz[N][N] de caractere
                """));
    }

    @Test
    public void aceitaRegistroEVetorDeRegistros() {
        analisar(comGlobais("""
                registro Aluno
                    nome: cadeia
                    notas: vetor[4] de real
                fim
                var turma: vetor[30] de Aluno
                """));
    }

    @Test
    public void aceitaVariaveisLocaisNoBlocoPrincipal() {
        analisar(comLocais("""
                var i: int = 0
                var ok: logico
                """));
    }

    @Test
    public void rejeitaConstanteSemTipo() {
        ErroSintatico e = erro(comGlobais("const MAX = 30\n"));
        assertEquals("esperado ':', encontrado '='", e.getMessage());
        assertEquals(2, e.getLinha());
        assertEquals(11, e.getColuna());
    }

    @Test
    public void rejeitaConstanteDeTipoNaoPrimitivo() {
        ErroSintatico e = erro(comGlobais("const V: vetor[3] de int = 1\n"));
        assertEquals("esperado um tipo primitivo, encontrado 'vetor'", e.getMessage());
    }

    @Test
    public void rejeitaVetorSemDe() {
        ErroSintatico e = erro(comGlobais("var v: vetor[5] int\n"));
        assertEquals("esperado 'de', encontrado 'int'", e.getMessage());
    }

    @Test
    public void rejeitaTamanhoReal() {
        ErroSintatico e = erro(comGlobais("var v: vetor[2.5] de int\n"));
        assertEquals("esperado um tamanho (inteiro ou constante), encontrado '2.5'", e.getMessage());
    }

    @Test
    public void rejeitaVetorDeVetor() {
        ErroSintatico e = erro(comGlobais("var v: vetor[2] de vetor[2] de int\n"));
        assertEquals("esperado um tipo (primitivo ou nome de registro), encontrado 'vetor'",
                e.getMessage());
    }

    @Test
    public void rejeitaRegistroSemCampos() {
        ErroSintatico e = erro(comGlobais("registro Vazio\nfim\n"));
        assertEquals("esperado um identificador, encontrado 'fim'", e.getMessage());
        assertEquals(3, e.getLinha());
    }

    @Test
    public void rejeitaDuasDeclaracoesNaMesmaLinha() {
        ErroSintatico e = erro(comGlobais("var a: int var b: int\n"));
        assertEquals("esperado fim de linha, encontrado 'var'", e.getMessage());
    }
}
