package ec.comp.util;

/**
 * Classificação de caracteres. Só aceita letras ASCII (A-Z, a-z),
 * exatamente como a regra "letra" da gramática léxica da Colombe.
 * (Character.isLetter aceitaria 'é', 'ç'... que a Colombe não permite em nomes.)
 */
public class AlphaNumericoUtil {

    private AlphaNumericoUtil() {
        // classe utilitária: só métodos estáticos, não deve ser instanciada
    }

    /** letra = "A" .. "Z" | "a" .. "z" */
    public static boolean isLetra(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    /** digito = "0" .. "9" */
    public static boolean isDigito(char c) {
        return c >= '0' && c <= '9';
    }

    public static boolean isAlphaNumerico(char c) {
        return isLetra(c) || isDigito(c);
    }

    /** Pode INICIAR um identificador: ( letra | "_" ) */
    public static boolean isInicioIdentificador(char c) {
        return isLetra(c) || c == '_';
    }

    /** Pode CONTINUAR um identificador: { letra | digito | "_" } */
    public static boolean isParteIdentificador(char c) {
        return isAlphaNumerico(c) || c == '_';
    }
}
