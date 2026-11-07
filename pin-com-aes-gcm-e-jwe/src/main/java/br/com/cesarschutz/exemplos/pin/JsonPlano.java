package br.com.cesarschutz.exemplos.pin;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lê e escreve o JSON pequeno e plano do JWE (o cabeçalho e o conteúdo): um objeto só, com valores texto ou número
 * inteiro, sem objetos nem listas dentro.
 *
 * <p>Num projeto de verdade, use uma biblioteca de JOSE madura (Nimbus JOSE + JWT, jose4j) e o JSON dela. Aqui o
 * exemplo fica só com o JDK para mostrar cada parte do JWE; o teste de interoperabilidade confere o resultado
 * contra uma implementação independente.
 */
final class JsonPlano {

    private JsonPlano() {
    }

    static String escrever(Map<String, Object> campos) {
        var sb = new StringBuilder("{");
        for (var e : campos.entrySet()) {
            if (sb.length() > 1) sb.append(',');
            sb.append('"').append(escapar(e.getKey())).append("\":");
            if (e.getValue() instanceof Number n) sb.append(n.longValue());
            else sb.append('"').append(escapar(String.valueOf(e.getValue()))).append('"');
        }
        return sb.append('}').toString();
    }

    static Map<String, Object> ler(String json) {
        var leitor = new Leitor(json.trim());
        return leitor.objeto();
    }

    private static String escapar(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static final class Leitor {
        private final String s;
        private int i;

        Leitor(String s) {
            this.s = s;
        }

        Map<String, Object> objeto() {
            var campos = new LinkedHashMap<String, Object>();
            esperar('{');
            pular();
            if (olhar() == '}') {
                i++;
                return fim(campos);
            }
            while (true) {
                pular();
                String chave = texto();
                pular();
                esperar(':');
                pular();
                Object valor = olhar() == '"' ? texto() : numero();
                if (campos.put(chave, valor) != null) throw erro("campo repetido: " + chave);
                pular();
                char c = s.charAt(i++);
                if (c == '}') return fim(campos);
                if (c != ',') throw erro("esperava , ou }");
            }
        }

        private Map<String, Object> fim(Map<String, Object> campos) {
            pular();
            if (i != s.length()) throw erro("sobrou texto depois do objeto");
            return campos;
        }

        private String texto() {
            esperar('"');
            var sb = new StringBuilder();
            while (true) {
                if (i >= s.length()) throw erro("texto sem fim");
                char c = s.charAt(i++);
                if (c == '"') return sb.toString();
                if (c == '\\') {
                    char e = s.charAt(i++);
                    switch (e) {
                        case '"', '\\', '/' -> sb.append(e);
                        case 'n' -> sb.append('\n');
                        case 't' -> sb.append('\t');
                        case 'u' -> {
                            sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                            i += 4;
                        }
                        default -> throw erro("escape desconhecido");
                    }
                } else sb.append(c);
            }
        }

        private Long numero() {
            int inicio = i;
            if (olhar() == '-') i++;
            while (i < s.length() && Character.isDigit(s.charAt(i))) i++;
            if (inicio == i) throw erro("esperava texto ou número");
            return Long.parseLong(s.substring(inicio, i));
        }

        private char olhar() {
            if (i >= s.length()) throw erro("fim inesperado");
            return s.charAt(i);
        }

        private void esperar(char c) {
            if (olhar() != c) throw erro("esperava " + c);
            i++;
        }

        private void pular() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        }

        private IllegalArgumentException erro(String o) {
            return new IllegalArgumentException("JSON inválido: " + o);
        }
    }
}
