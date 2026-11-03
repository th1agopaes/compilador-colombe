package ec.comp.model;

/**
 * Categorias de token da linguagem Colombe.
 * Seguem a regra "token" da gramática léxica (especificação EBNF, Seção 3).
 */
public enum TipoToken {
    PALAVRA_RESERVADA,   // programa, inicio, se, enquanto, e, ou, verdadeiro...
    IDENTIFICADOR,       // nomes criados pelo programador: soma, Aluno, _x1
    LITERAL_INTEIRO,     // 42
    LITERAL_REAL,        // 3.14
    LITERAL_CARACTERE,   // 'a'  '\n'
    LITERAL_CADEIA,      // "Ana Banana\n"
    OPERADOR,            // ++ -- == != <= >= + - * / ^ < > =
    DELIMITADOR,         // ( ) [ ] , : .
    NL,                  // quebra de linha = terminador de instrução
    ERRO,                // trecho que não forma nenhum token válido
    FIM                  // fim do arquivo (EOF)
}
