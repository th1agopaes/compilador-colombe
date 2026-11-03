# Especificação léxica da Colombe

Trecho da especificação formal da linguagem (gramática livre de contexto em EBNF),
seções de **convenções léxicas** e **gramática léxica**. É exatamente o que o
`AnalisadorLexico` implementa.

## Notação EBNF

| Símbolo            | Significado                            |
|:------------------:|----------------------------------------|
| `=`                | definição                              |
| `,`                | concatenação                           |
| `\|`               | alternativa (OU)                       |
| `[ ... ]`          | opcional (zero ou uma vez)             |
| `{ ... }`          | repetição (zero ou mais vezes)         |
| `( ... )`          | agrupamento                            |
| `"..."` / `'...'`  | símbolo terminal                       |
| `(* ... *)`        | comentário                             |
| `? ... ?`          | descrição em linguagem natural         |
| `"a" .. "z"`       | faixa de caracteres                    |

## Convenções léxicas

1. **Case-sensitive:** `Total` ≠ `total`.
2. **Espaços e tabulações** separam tokens e são descartados.
3. **Comentários** `// ...` e `/* ... */` são descartados. Comentários de bloco não se aninham.
4. **Quebra de linha é o terminador de instrução.** O léxico emite o token `NL` e *agrupa*
   quebras consecutivas (linhas em branco e linhas só com comentário) em um único `NL`.
5. **Maior casamento** (*maximal munch*): o léxico forma sempre o token mais longo possível.
   `a--b` gera `a`, `--`, `b`; para subtrair um negativo escreve-se `a - -b`.
6. **Palavras reservadas** (40) não podem ser usadas como identificadores:

```
programa inicio fim var const int real logico caractere cadeia vazio
registro vetor matriz modulo retorna ref se entao senao fimse
enquanto faca fimenquanto repita ate para de passo fimpara
e ou nao xou div mod verdadeiro falso leia escreve
```

## Gramática léxica

```ebnf
(* ---------- Caracteres básicos ---------- *)
letra   = "A" .. "Z" | "a" .. "z" ;
digito  = "0" .. "9" ;

(* ---------- Identificadores ---------- *)
identificador = ( letra | "_" ) , { letra | digito | "_" } ;
                (* exceto as palavras reservadas *)

(* ---------- Literais ---------- *)
literal_inteiro   = digito , { digito } ;
literal_real      = digito , { digito } , "." , digito , { digito } ;
literal_logico    = "verdadeiro" | "falso" ;
literal_caractere = "'" , ( caractere_simples | escape ) , "'" ;
literal_cadeia    = '"' , { caractere_de_cadeia | escape } , '"' ;

escape = "\" , ( "n" | "t" | '"' | "'" | "\" ) ;

caractere_simples   = ? qualquer caractere exceto "'", "\" e quebra de linha ? ;
caractere_de_cadeia = ? qualquer caractere exceto '"', "\" e quebra de linha ? ;

(* ---------- Terminador de instrução ---------- *)
quebra_linha = ? caractere de nova linha (LF ou CR LF) ? ;
NL           = quebra_linha , { quebra_linha } ;
               (* linhas vazias e de comentário são absorvidas *)

(* ---------- Elementos descartados ---------- *)
espaco           = " " | ? tabulação ? ;
comentario_linha = "//" , { ? qualquer caractere exceto quebra de linha ? } ;
comentario_bloco = "/*" , { ? qualquer caractere ? } , "*/" ;

(* ---------- Operadores e delimitadores ---------- *)
operador    = "++" | "--" | "==" | "!=" | "<=" | ">="
            | "+" | "-" | "*" | "/" | "^" | "<" | ">" | "=" ;
delimitador = "(" | ")" | "[" | "]" | "," | ":" | "." ;

(* ---------- Token: unidade entregue ao analisador sintático ---------- *)
token = palavra_reservada | identificador | literal_real | literal_inteiro
      | literal_caractere | literal_cadeia | operador | delimitador | NL ;
```

**Ordem de reconhecimento:** `"++"` vence `"+"`, `"<="` vence `"<"`, `3.14` é um
`literal_real` (e não `3`, `.`, `14`) e `inicio2` é um identificador (e não `inicio` + `2`).
Quando uma palavra casa com `identificador` e com `palavra_reservada`, vale a palavra
reservada. O sinal de negativo (`-5`) **não** faz parte do literal: é o operador unário `-`,
tratado na gramática sintática.

## Decisões de implementação do léxico

Pontos que a gramática não fixa e que o analisador decidiu:

| Situação | Decisão |
|---|---|
| `/* ... */` que ocupa várias linhas | conta como uma quebra de linha (gera `NL`) |
| `2nota` | um único erro de "número malformado" (e não `2` + `nota`) |
| `média` (letra acentuada) | um único erro para a palavra inteira |
| `verdadeiro` / `falso` | saem como `PALAVRA_RESERVADA` (o sintático as trata como `literal_logico`) |
| Tabulação | conta como 1 coluna na posição do token |
| Arquivo com `\r\n` (Windows) ou BOM UTF-8 | tratados de forma transparente |
| Erro léxico | registrado, vira token `ERRO`, e a análise **continua** até o fim do arquivo |
