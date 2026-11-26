package agenda;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;

/**
 * Acesso ao MongoDB. Operações do Banco
 * Banco: "agenda_db" | Coleção: "compromissos"
 */
public class AgendaBanco implements AutoCloseable {

    private final MongoClient cliente;
    private final MongoCollection<Document> colecao;

    public AgendaBanco(String uri, String nomeBanco) {
        MongoClientSettings config = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(uri))
                // Se o MongoDB não estiver rodando, desiste em 5 segundos
                .applyToClusterSettings(b -> b.serverSelectionTimeout(5, TimeUnit.SECONDS))
                .build();
        this.cliente = MongoClients.create(config);
        this.colecao = cliente.getDatabase(nomeBanco).getCollection("compromissos");
    }

    /** Envia um "ping" ao servidor; lança exceção se não conseguir conectar. */
    public void testarConexao() {
        cliente.getDatabase("admin").runCommand(new Document("ping", 1));
    }

    //Cadastro
    public void inserir(Compromisso c) {
        Document doc = new Document("dataHora", paraDate(c.getDataHora()))
                .append("titulo", c.getTitulo())
                .append("descricao", c.getDescricao())
                .append("pessoas", c.getPessoas()); // vira um array no documento
        colecao.insertOne(doc);
    }

    //Lista
    public List<Compromisso> listar() {
        List<Compromisso> lista = new ArrayList<>();
        for (Document doc : colecao.find().sort(Sorts.ascending("dataHora"))) {
            lista.add(paraCompromisso(doc));
        }
        return lista;
    }

    //Altera titulo e descrição
    public boolean alterarTituloDescricao(ObjectId id, String titulo, String descricao) {
        return colecao.updateOne(
                Filters.eq("_id", id),
                Updates.combine(Updates.set("titulo", titulo), Updates.set("descricao", descricao))
        ).getMatchedCount() > 0;
    }

    //Altera pessoas
    /** Adiciona uma pessoa ao final do array */
    public boolean adicionarPessoa(ObjectId id, String pessoa) {
        return colecao.updateOne(
                Filters.eq("_id", id),
                Updates.push("pessoas", pessoa)
        ).getMatchedCount() > 0;
    }

    /** Troca o array inteiro por uma nova lista */
    public boolean substituirPessoas(ObjectId id, List<String> pessoas) {
        return colecao.updateOne(
                Filters.eq("_id", id),
                Updates.set("pessoas", pessoas)
        ).getMatchedCount() > 0;
    }

    // Exclui pessoa
    /** Remove um item do array */
    public boolean removerPessoa(ObjectId id, String pessoa) {
        return colecao.updateOne(
                Filters.eq("_id", id),
                Updates.pull("pessoas", pessoa)
        ).getMatchedCount() > 0;
    }

    // Deleta compromisso
    public boolean excluir(ObjectId id) {
        return colecao.deleteOne(Filters.eq("_id", id)).getDeletedCount() > 0;
    }

    @Override
    public void close() {
        cliente.close();
    }

    // Conversões
    private Compromisso paraCompromisso(Document doc) {
        return new Compromisso(
                doc.getObjectId("_id"),
                paraLocalDateTime(doc.getDate("dataHora")),
                doc.getString("titulo"),
                doc.getString("descricao"),
                doc.getList("pessoas", String.class)
        );
    }

    private Date paraDate(LocalDateTime dataHora) {
        return Date.from(dataHora.atZone(ZoneId.systemDefault()).toInstant());
    }

    private LocalDateTime paraLocalDateTime(Date data) {
        return (data == null) ? null : LocalDateTime.ofInstant(data.toInstant(), ZoneId.systemDefault());
    }
}
