# Compilador Colombe

[![CI](https://github.com/th1agopaes/compilador-colombe/actions/workflows/ci.yml/badge.svg)](https://github.com/th1agopaes/compilador-colombe/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-17%2B-orange)
![Maven](https://img.shields.io/badge/build-Maven-blue)

Compilador da **Colombe** (`.cb`), uma linguagem de programação procedural em português,
com módulos, registros, vetores e matrizes. Projeto da disciplina de **Compiladores** de
Engenharia da Computação no IFMT, Campus Cuiabá Octayde Jorge da Silva.

```
programa Ola
inicio
	var nome: cadeia
	leia(nome)
	escreve("Olá, ", nome, "!\n")
fim
```

## Fases do compilador

| Fase | Status | Versão |
|---|---|---|
| Especificação formal (EBNF) | ✅ concluída | — |
| **Analisador léxico** | ✅ concluído | `v0.1.0` |
| Analisador sintático | ⏳ próxima etapa | — |
| Analisador semântico | ⏳ | — |
| Geração de código | ⏳ | — |

## Analisador léxico

O analisador léxico lê o código-fonte caractere por caractere e o agrupa em **tokens**,
como um caixa de supermercado que passa os produtos um a um e identifica cada um pelo
código de barras.

- Implementa a [gramática léxica da Colombe](docs/especificacao-lexica.md).
- Usa a regra do **maior casamento**: `a--b` vira `a`, `--`, `b`.
- Informa **linha e coluna** de cada token.
- **Não para no primeiro erro.** Lista todos os erros do arquivo, com dicas para quem vem
  de C/Java (por exemplo: "a Colombe não usa ';'").
- Aceita arquivos do Windows (`\r\n`, BOM, ANSI) e do Linux.

| Token | Exemplos |
|---|---|
| `PALAVRA_RESERVADA` | `programa` `inicio` `se` `enquanto` `e` `ou` `nao` `div` `verdadeiro` |
| `IDENTIFICADOR` | `soma` `Aluno` `_x1` |
| `LITERAL_INTEIRO` / `LITERAL_REAL` | `42` / `3.14` |
| `LITERAL_CARACTERE` / `LITERAL_CADEIA` | `'a'` `'\n'` / `"Ana\n"` |
| `OPERADOR` | `++ -- == != <= >= + - * / ^ < > =` |
| `DELIMITADOR` | `( ) [ ] , : .` |
| `NL` | quebra de linha (terminador de instrução) |
| `ERRO` | trecho inválido (detalhado no fim da saída) |

## Como executar

**Requisitos:** JDK 17 ou superior e Maven 3.6.3 ou superior.

```bash
# 1. Compilar, rodar os testes e gerar target/colombe.jar
mvn package

# 2. Analisar um arquivo informado na linha de comando
java -jar target/colombe.jar exemplos/registro-alunos.cb

# 3. Ou rodar sem argumento: o programa pergunta o nome do arquivo
java -jar target/colombe.jar
```

Código de saída: `0` sem erros, `1` com erros léxicos, `2` erro de uso ou de arquivo.

### Exemplo de saída

```
==============================================================
 Analisador Léxico - Linguagem Colombe
 Arquivo: exemplos/registro-alunos.cb
==============================================================
 LIN:COL   TOKEN
   1:1     <PALAVRA_RESERVADA, "programa">
   1:10    <IDENTIFICADOR, "RegistroAlunos">
   1:24    <NL, "\n">
   3:1     <PALAVRA_RESERVADA, "const">
   3:7     <IDENTIFICADOR, "MAX_ALUNOS">
   ...
--------------------------------------------------------------
 Resumo: 485 tokens
   PALAVRA_RESERVADA     70
   IDENTIFICADOR        102
   ...
--------------------------------------------------------------
 Análise léxica concluída sem erros.
```

Com erros (`exemplos/erros-lexicos.cb`):

```
 13 erro(s) léxico(s) encontrado(s):
   Erro léxico [linha 5, coluna 6]: identificador 'média' tem caractere não permitido (use só letras sem acento, dígitos e '_')
   Erro léxico [linha 7, coluna 17]: caractere inválido ';' (a Colombe não usa ';': a instrução termina na quebra de linha)
   Erro léxico [linha 17, coluna 10]: cadeia não terminada: falta o " de fechamento antes do fim da linha
   ...
```

## Programas de exemplo

| Arquivo | Conteúdo |
|---|---|
| [`registro-alunos.cb`](exemplos/registro-alunos.cb) | programa exemplo da linguagem: registros, vetor de registros, módulos com parâmetro por valor e por referência |
| [`todos-os-tokens.cb`](exemplos/todos-os-tokens.cb) | usa todas as categorias de token (sem erros) |
| [`erros-lexicos.cb`](exemplos/erros-lexicos.cb) | um erro léxico proposital por linha |

## Estrutura do projeto

```
src/main/java/ec/comp/
├── App.java                      # linha de comando: lê o arquivo e imprime os tokens
├── analisador/
│   ├── AnalisadorLexico.java     # o scanner: nextToken()
│   └── PalavrasReservadas.java   # as 40 palavras reservadas (Set, busca O(1))
├── model/
│   ├── TipoToken.java            # categorias de token
│   ├── Token.java                # tipo + lexema + linha + coluna (imutável)
│   └── ErroLexico.java           # mensagem + linha + coluna
└── util/AlphaNumericoUtil.java   # classificação de caracteres
src/test/java/ec/comp/            # testes JUnit (unitários + exemplos + linha de comando)
exemplos/                         # programas .cb
docs/                             # especificação léxica (EBNF)
```

## Testes

```bash
mvn test
```

São 30 testes, que cobrem maior casamento, escapes, `\r\n`, posição dos tokens,
recuperação de erros, os três programas de exemplo e a linha de comando.
O [GitHub Actions](.github/workflows/ci.yml) roda todos a cada push, no Java 17 e no 21.

## Autor

**Thiago Paes** ([@th1agopaes](https://github.com/th1agopaes)), Engenharia da Computação, IFMT.
Disciplina de Compiladores, prof. Evandro César Freiberger.
