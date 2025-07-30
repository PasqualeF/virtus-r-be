


/*

// 6. ENTITÀ PER TRACKING NOTIFICHE
@Entity
@Table(name = "notifiche_inviate")
public class NotificaInviata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tipo", nullable = false)
    private String tipo; // "30_GIORNI", "15_GIORNI", "7_GIORNI"

    @Column(name = "data_invio", nullable = false)
    private LocalDate dataInvio;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public LocalDate getDataInvio() { return dataInvio; }
    public void setDataInvio(LocalDate dataInvio) { this.dataInvio = dataInvio; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}

// 7. REPOSITORY
@Repository
public interface NotificaRepository extends JpaRepository<NotificaInviata, Long> {
    boolean existsByTipoAndDataInvio(String tipo, LocalDate dataInvio);
}
*/