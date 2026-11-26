package agenda;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;

//Representa um compromisso na agenda.

public class Compromisso {

    // STRICT impede datas inexistentes, como 31/02/2026
    public static final DateTimeFormatter FORMATO =
            DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm").withResolverStyle(ResolverStyle.STRICT);

    private final ObjectId id;
    private final LocalDateTime dataHora;
    private final String titulo;
    private final String descricao;
    private final List<String> pessoas;

    // Construtor para um compromisso novo (o MongoDB gera o _id ao inserir)
    public Compromisso(LocalDateTime dataHora, String titulo, String descricao, List<String> pessoas) {
        this(null, dataHora, titulo, descricao, pessoas);
    }

    // Construtor para um compromisso lido do banco
    public Compromisso(ObjectId id, LocalDateTime dataHora, String titulo, String descricao, List<String> pessoas) {
        this.id = id;
        this.dataHora = dataHora;
        this.titulo = titulo;
        this.descricao = descricao;
        this.pessoas = (pessoas == null) ? new ArrayList<>() : new ArrayList<>(pessoas);
    }

    public ObjectId getId() {
        return id;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public String getDataHoraFormatada() {
        return (dataHora == null) ? "-" : dataHora.format(FORMATO);
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public List<String> getPessoas() {
        return pessoas;
    }
}
