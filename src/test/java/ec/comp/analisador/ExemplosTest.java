package ec.comp.analisador;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import ec.comp.model.TipoToken;
import ec.comp.model.Token;

/**
 * Testes de integração: rodam o léxico nos programas da pasta exemplos/.
 * Garantem que o exemplo entregue ao professor continua válido a cada mudança.
 * (O Maven executa os testes a partir da pasta do projeto, por isso o caminho relativo.)
 */
public class ExemplosTest {

    private static AnalisadorLexico analisar(String nomeArquivo) throws IOException {
        String codigo = Files.readString(Path.of("exemplos", nomeArquivo), StandardCharsets.UTF_8);
        return new AnalisadorLexico(codigo);
    }

    @Test
    public void programaExemploNaoTemErros() throws IOException {
        AnalisadorLexico lex = analisar("registro-alunos.cb");
        lex.analisarTudo();
        assertFalse("erros: " + lex.getErros(), lex.temErros());
    }

    @Test
    public void programaExemploEstaSintaticamenteCorreto() throws IOException {
        AnalisadorLexico lex = analisar("registro-alunos.cb");
        List<Token> tokens = lex.analisarTudo();
        new AnalisadorSintatico(tokens).parsePrograma();   // lança ErroSintatico se houver erro
    }

    @Test
    public void todosOsTokensUsaTodasAsCategoriasValidas() throws IOException {
        AnalisadorLexico lex = analisar("todos-os-tokens.cb");
        List<Token> tokens = lex.analisarTudo();
        assertFalse("erros: " + lex.getErros(), lex.temErros());

        Set<TipoToken> encontrados = EnumSet.noneOf(TipoToken.class);
        tokens.forEach(t -> encontrados.add(t.getTipoToken()));

        // todas as categorias, menos ERRO (o arquivo é válido)
        Set<TipoToken> esperados = EnumSet.complementOf(EnumSet.of(TipoToken.ERRO));
        assertEquals(esperados, encontrados);
    }

    @Test
    public void arquivoDeErrosTemOsTrezeErrosPropositais() throws IOException {
        AnalisadorLexico lex = analisar("erros-lexicos.cb");
        List<Token> tokens = lex.analisarTudo();
        assertEquals("erros: " + lex.getErros(), 13, lex.getErros().size());
        // mesmo com erros, a análise vai até o fim do arquivo
        assertTrue(tokens.get(tokens.size() - 1).getTipoToken() == TipoToken.FIM);
    }
}
