package ec.comp.analisador;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import ec.comp.model.TipoToken;
import ec.comp.model.Token;

/**
 * Testes do analisador léxico. Rodam sozinhos no "mvn package" (fase test)
 * e garantem que uma mudança futura não quebre o que já funcionava.
 */
public class AnalisadorLexicoTest {

    // ---------- auxiliares ----------

    private static List<Token> tokens(String codigo) {
        return new AnalisadorLexico(codigo).analisarTudo();
    }

    /** Tipos dos tokens, sem o FIM final. */
    private static List<TipoToken> tipos(String codigo) {
        List<Token> lista = tokens(codigo);
        return lista.subList(0, lista.size() - 1).stream()
                .map(Token::getTipoToken).collect(Collectors.toList());
    }

    /** Lexemas dos tokens, sem o FIM final. */
    private static List<String> lexemas(String codigo) {
        List<Token> lista = tokens(codigo);
        return lista.subList(0, lista.size() - 1).stream()
                .map(Token::getToken).collect(Collectors.toList());
    }

    // ---------- identificadores e palavras reservadas ----------

    @Test
    public void palavraReservadaVersusIdentificador() {
        assertEquals(List.of(TipoToken.PALAVRA_RESERVADA, TipoToken.IDENTIFICADOR,
                TipoToken.IDENTIFICADOR, TipoToken.IDENTIFICADOR),
                tipos("inicio inicio2 Se _x1"));
    }

    @Test
    public void operadoresPorExtensoSaoPalavrasReservadas() {
        for (String p : List.of("e", "ou", "nao", "xou", "div", "mod", "verdadeiro", "falso")) {
            assertEquals(List.of(TipoToken.PALAVRA_RESERVADA), tipos(p));
        }
    }

    @Test
    public void todasAsPalavrasReservadasDaEspecificacao() {
        assertEquals(40, PalavrasReservadas.quantidade());
    }

    // ---------- números ----------

    @Test
    public void inteiroERealCorrigindoBugDoExemplo() {
        // no exemplo original "10" virava IDENTIFICADOR
        assertEquals(List.of(TipoToken.LITERAL_INTEIRO, TipoToken.LITERAL_REAL), tipos("10 3.14"));
    }

    @Test
    public void pontoSemDigitoNaoFazParteDoNumero() {
        assertEquals(List.of("3", ".", "x"), lexemas("3.x"));
    }

    @Test
    public void negativoNaoFazParteDoLiteral() {
        assertEquals(List.of("-", "5"), lexemas("-5"));
    }

    @Test
    public void numeroMalformado() {
        AnalisadorLexico lex = new AnalisadorLexico("2nota");
        assertEquals(TipoToken.ERRO, lex.nextToken().getTipoToken());
        assertEquals(1, lex.getErros().size());
    }

    // ---------- operadores: maior casamento ----------

    @Test
    public void maiorCasamento() {
        assertEquals(List.of("a", "--", "b"), lexemas("a--b"));
        assertEquals(List.of("a", "-", "-", "b"), lexemas("a - -b"));
        assertEquals(List.of("x", "++", "+", "++", "x"), lexemas("x++ + ++x"));
        assertEquals(List.of("<=", ">=", "==", "!=", "<", ">", "=", "^"), lexemas("<= >= == != < > = ^"));
    }

    @Test
    public void delimitadores() {
        assertEquals(List.of("t", "[", "0", "]", ".", "n", "(", ")", ",", ":"), lexemas("t[0].n(),:"));
    }

    // ---------- literais de caractere e cadeia ----------

    @Test
    public void caractereECadeiaComEscapes() {
        assertEquals(List.of(TipoToken.LITERAL_CARACTERE, TipoToken.LITERAL_CARACTERE, TipoToken.LITERAL_CADEIA),
                tipos("'a' '\\n' \"ela disse \\\"oi\\\"\\t!\""));
    }

    @Test
    public void caractereComEmojiContaComoUmCaractere() {
        // '😀' ocupa 2 chars em Java (par substituto), mas é 1 caractere
        assertEquals(List.of(TipoToken.LITERAL_CARACTERE), tipos("'😀'"));
    }

    @Test
    public void cadeiaNaoTerminada() {
        AnalisadorLexico lex = new AnalisadorLexico("\"sem fim\nx");
        assertEquals(TipoToken.ERRO, lex.nextToken().getTipoToken());
        assertEquals(TipoToken.NL, lex.nextToken().getTipoToken());
        assertEquals(TipoToken.IDENTIFICADOR, lex.nextToken().getTipoToken());
        assertEquals(1, lex.getErros().size());
    }

    @Test
    public void errosDeCaractere() {
        assertEquals(List.of(TipoToken.ERRO, TipoToken.ERRO, TipoToken.ERRO), tipos("'' 'ab' '\\q'"));
    }

    // ---------- NL e comentários ----------

    @Test
    public void quebrasSeguidasViramUmNL() {
        String codigo = "a\n\n   \n// comentario\n\r\nb\n";
        assertEquals(List.of(TipoToken.IDENTIFICADOR, TipoToken.NL, TipoToken.IDENTIFICADOR, TipoToken.NL),
                tipos(codigo));
    }

    @Test
    public void comentarioDeBlocoEmVariasLinhasSeparaInstrucoes() {
        assertEquals(List.of(TipoToken.IDENTIFICADOR, TipoToken.NL, TipoToken.IDENTIFICADOR),
                tipos("a /* x\n y */ b"));
        assertEquals(List.of(TipoToken.IDENTIFICADOR, TipoToken.IDENTIFICADOR), tipos("a /* x */ b"));
    }

    @Test
    public void comentarioDeBlocoNaoTerminado() {
        AnalisadorLexico lex = new AnalisadorLexico("a /* nunca fecha");
        lex.analisarTudo();
        assertTrue(lex.temErros());
    }

    // ---------- posição ----------

    @Test
    public void linhaEColunaComCRLF() {
        List<Token> t = tokens("programa X\r\n\tvar y");
        Token var = t.get(3); // programa, X, NL, var
        assertEquals("var", var.getToken());
        assertEquals(2, var.getLinha());
        assertEquals(2, var.getColuna()); // tab conta como 1 coluna
    }

    // ---------- caracteres inválidos ----------

    @Test
    public void caracteresInvalidosNaoParamAAnalise() {
        AnalisadorLexico lex = new AnalisadorLexico("x = 1; y @ 2 média");
        List<Token> t = lex.analisarTudo();
        assertEquals(TipoToken.FIM, t.get(t.size() - 1).getTipoToken());
        assertEquals(3, lex.getErros().size()); // ';'  '@'  'média'
    }

    @Test
    public void programaValidoSemErros() {
        String codigo = "programa P\nvar a: vetor[3] de int\ninicio\n  a[0] = 2 ^ 3 div 2\nfim\n";
        AnalisadorLexico lex = new AnalisadorLexico(codigo);
        lex.analisarTudo();
        assertFalse(lex.temErros());
    }
}
