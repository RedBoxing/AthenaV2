package fr.redboxing.athena.database.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "servers")
@AllArgsConstructor
@NoArgsConstructor
public class Server {
    @Id
    @Getter
    @Setter
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Getter
    @Setter
    @Column(name = "address", nullable = false)
    private String address;

    @Getter
    @Setter
    @Column(name = "version", nullable = false)
    private String version;

    @Getter
    @Setter
    @Column(name = "description", nullable = true)
    private String description;

    @Getter
    @Setter
    @Column(name = "onlinePlayers", nullable = false)
    private Integer onlinePlayers;

    @Getter
    @Setter
    @Column(name = "maxPlayers", nullable = false)
    private Integer maxPlayers;

    @Getter
    @Setter
    @Column(name = "modLoader", nullable = false)
    private String modLoader;

    @Getter
    @Setter
    @Column(name = "players", nullable = true)
    private String players;

    @Getter
    @Setter
    @Column(name = "online", nullable = false)
    private boolean online;

    @Getter
    @Setter
    @Column(name = "lastOnline", nullable = false)
    private Long lastOnline;
}
