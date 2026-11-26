package agenda;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import io.github.cdimascio.dotenv.Dotenv;


public class Main {

    // Guarda a referência para o ajuste de log não ser descartado pelo Java
    private static final Logger LOG_MONGO = Logger.getLogger("org.mongodb.driver");

    private static Scanner teclado;
    private static AgendaBanco agendaMongo;

    public static void main(String[] args) {
        // Garante acentos corretos no console
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        // Esconde as mensagens informativas do driver, deixando só os erros
        LOG_MONGO.setLevel(Level.SEVERE);
        teclado = new Scanner(System.in, StandardCharsets.UTF_8);

        // Carrega as configurações do arquivo .env
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();

        // Primeiro procura a variável de ambiente do sistema
        String uri = System.getenv("MONGO_URI");

        // Se não encontrar, procura no arquivo .env
        if (uri == null || uri.isBlank()) {
            uri = dotenv.get("MONGO_URI");
        }

        // Se não encontrar em nenhum dos dois, utiliza o MongoDB local
        if (uri == null || uri.isBlank()) {
            uri = "mongodb://localhost:27017";
        }

        try (AgendaBanco agenda = new AgendaBanco(uri, "agenda_db")) {
            agendaMongo = agenda;
            try {
                agendaMongo.testarConexao();
            } catch (Exception e) {
                System.out.println("Não foi possível conectar ao MongoDB.");
                System.out.println("Verifique se o serviço está em execução!");
                return;
            }
            menu();
        }
    }

    private static void menu() {
        int opcao;
        do {
            System.out.println();
            System.out.println("|------------- AGENDA -------------|");
            System.out.println("1 - Listar compromissos");
            System.out.println("2 - Cadastrar novo compromisso");
            System.out.println("3 - Alterar título e descrição");
            System.out.println("4 - Alterar pessoas envolvidas");
            System.out.println("5 - Excluir uma pessoa de um compromisso");
            System.out.println("6 - Excluir um compromisso");
            System.out.println("0 - Sair");
            opcao = lerInteiro("Escolha uma opção: ");


            switch (opcao) {
                case 1 -> listar();
                case 2 -> cadastrar();
                case 3 -> alterarTituloDescricao();
                case 4 -> alterarPessoas();
                case 5 -> excluirPessoa();
                case 6 -> excluirCompromisso();
                case 0 -> System.out.println("Encerrando...");
                default -> System.out.println("Opção inválida.");
            }
        } while (opcao != 0);
    }

    // ---------- FUNCIONALIDADES ----------

    private static void listar() {
        List<Compromisso> lista = agendaMongo.listar();
        if (lista.isEmpty()) {
            System.out.println("Nenhum compromisso cadastrado.");
            return;
        }
        exibir(lista);
    }

    private static void cadastrar() {
        LocalDateTime dataHora = lerDataHora();
        String titulo = lerTextoObrigatorio("Título: ");
        System.out.print("Descrição: ");
        String descricao = teclado.nextLine().trim();
        List<String> pessoas = lerPessoas();

        agendaMongo.inserir(new Compromisso(dataHora, titulo, descricao, pessoas));
        System.out.println("Compromisso cadastrado!");
    }

    private static void alterarTituloDescricao() {
        Compromisso c = escolherCompromisso();
        if (c == null) {
            return;
        }
        System.out.println("Deixe em branco para manter o valor atual.");

        System.out.print("Novo título [" + c.getTitulo() + "]: ");
        String titulo = teclado.nextLine().trim();
        if (titulo.isEmpty()) {
            titulo = c.getTitulo();
        }

        System.out.print("Nova descrição [" + c.getDescricao() + "]: ");
        String descricao = teclado.nextLine().trim();
        if (descricao.isEmpty()) {
            descricao = c.getDescricao();
        }

        boolean ok = agendaMongo.alterarTituloDescricao(c.getId(), titulo, descricao);
        System.out.println(ok ? "Compromisso atualizado!" : "Compromisso não encontrado.");
    }

    private static void alterarPessoas() {
        Compromisso c = escolherCompromisso();
        if (c == null) {
            return;
        }
        System.out.println("1 - Adicionar uma pessoa");
        System.out.println("2 - Substituir toda a lista de pessoas");
        int opcao = lerInteiro("Escolha: ");

        if (opcao == 1) {
            String nome = lerTextoObrigatorio("Nome da pessoa: ");
            agendaMongo.adicionarPessoa(c.getId(), nome);
            System.out.println("Pessoa adicionada!");
        } else if (opcao == 2) {
            List<String> novas = lerPessoas();
            agendaMongo.substituirPessoas(c.getId(), novas);
            System.out.println("Lista de pessoas substituída!");
        } else {
            System.out.println("Opção inválida.");
        }
    }

    private static void excluirPessoa() {
        Compromisso c = escolherCompromisso();
        if (c == null) {
            return;
        }
        List<String> pessoas = c.getPessoas();
        if (pessoas.isEmpty()) {
            System.out.println("Este compromisso não tem pessoas cadastradas.");
            return;
        }
        for (int i = 0; i < pessoas.size(); i++) {
            System.out.println((i + 1) + " - " + pessoas.get(i));
        }
        int n = lerInteiro("Número da pessoa a excluir (0 para cancelar): ");
        if (n < 1 || n > pessoas.size()) {
            System.out.println("Operação cancelada.");
            return;
        }
        agendaMongo.removerPessoa(c.getId(), pessoas.get(n - 1));
        System.out.println("Pessoa removida!");
    }

    private static void excluirCompromisso() {
        Compromisso c = escolherCompromisso();
        if (c == null) {
            return;
        }
        System.out.print("Excluir \"" + c.getTitulo() + "\"? (s/n): ");
        String resposta = teclado.nextLine().trim();
        if (resposta.equalsIgnoreCase("s")) {
            boolean ok = agendaMongo.excluir(c.getId());
            System.out.println(ok ? "Compromisso excluído!" : "Compromisso não encontrado.");
        } else {
            System.out.println("Operação cancelada.");
        }
    }

    // ---------- APOIO ----------

    private static void exibir(List<Compromisso> lista) {
        for (int i = 0; i < lista.size(); i++) {
            Compromisso c = lista.get(i);
            System.out.println();
            System.out.println("[" + (i + 1) + "] " + c.getTitulo());
            System.out.println("    Data/Horário: " + c.getDataHoraFormatada());
            System.out.println("    Descrição...: " + c.getDescricao());
            System.out.println("    Pessoas.....: "
                    + (c.getPessoas().isEmpty() ? "(ninguém)" : String.join(", ", c.getPessoas())));
        }
    }

    // Mostra a lista numerada e devolve o compromisso escolhido
    private static Compromisso escolherCompromisso() {
        List<Compromisso> lista = agendaMongo.listar();
        if (lista.isEmpty()) {
            System.out.println("Nenhum compromisso cadastrado.");
            return null;
        }
        exibir(lista);
        System.out.println();
        int n = lerInteiro("Número do compromisso (0 para cancelar): ");
        if (n < 1 || n > lista.size()) {
            System.out.println("Operação cancelada.");
            return null;
        }
        return lista.get(n - 1);
    }

    // Lê nomes um a um até o usuário deixar a linha em branco.
    private static List<String> lerPessoas() {
        List<String> pessoas = new ArrayList<>();
        System.out.println("Digite uma pessoa por vez (Enter em branco para terminar):");
        while (true) {
            System.out.print("Pessoa: ");
            String nome = teclado.nextLine().trim();
            if (nome.isEmpty()) {
                break;
            }
            pessoas.add(nome);
        }
        return pessoas;
    }

    private static LocalDateTime lerDataHora() {
        while (true) {
            System.out.print("Data e horário (dd/MM/aaaa HH:mm): ");
            try {
                return LocalDateTime.parse(teclado.nextLine().trim(), Compromisso.FORMATO);
            } catch (DateTimeParseException e) {
                System.out.println("Formato inválido. Exemplo: 25/12/2026 12:00");
            }
        }
    }

    private static String lerTextoObrigatorio(String mensagem) {
        while (true) {
            System.out.print(mensagem);
            String texto = teclado.nextLine().trim();
            if (!texto.isEmpty()) {
                return texto;
            }
            System.out.println("Este campo é obrigatório!");
        }
    }

    private static int lerInteiro(String mensagem) {
        System.out.print(mensagem);
        try {
            return Integer.parseInt(teclado.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
