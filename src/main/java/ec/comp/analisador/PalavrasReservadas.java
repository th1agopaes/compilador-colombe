package ec.comp.analisador;

import java.util.Set;

/**
 * Tabela de palavras reservadas da Colombe (especificação EBNF, Seção 2, item 6).
 *
 * Usamos um Set (conjunto com hash): a busca é O(1) em média, em vez de
 * percorrer um vetor inteiro a cada identificador lido (O(n)), como no exemplo original.
 */
public final class PalavrasReservadas {

    private static final Set<String> PALAVRAS = Set.of(
        // estrutura do programa e declarações
        "programa", "inicio", "fim", "var", "const",
        // tipos
        "int", "real", "logico", "caractere", "cadeia", "vazio",
        "registro", "vetor", "matriz",
        // módulos
        "modulo", "retorna", "ref",
        // seleção
        "se", "entao", "senao", "fimse",
        // repetição
        "enquanto", "faca", "fimenquanto", "repita", "ate",
        "para", "de", "passo", "fimpara",
        // operadores escritos por extenso
        "e", "ou", "nao", "xou", "div", "mod",
        // literais lógicos
        "verdadeiro", "falso",
        // entrada e saída
        "leia", "escreve"
    );

    private PalavrasReservadas() {
    }

    /** A Colombe é case-sensitive: "Se" NÃO é palavra reservada, é identificador. */
    public static boolean isPalavraReservada(String palavra) {
        return PALAVRAS.contains(palavra);
    }

    public static int quantidade() {
        return PALAVRAS.size();
    }
}
