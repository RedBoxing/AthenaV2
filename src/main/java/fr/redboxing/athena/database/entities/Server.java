package fr.redboxing.athena.database.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
public class Server {
    @Getter
    @Setter
    private Long id;

    @Getter
    @Setter
    private String host;

    @Getter
    @Setter
    private int port;

    @Getter
    @Setter
    private String version;

    @Getter
    @Setter
    private String description;

    @Getter
    @Setter
    private Integer onlinePlayers;

    @Getter
    @Setter
    private Integer maxPlayers;

    @Getter
    @Setter
    private String modloader;

    @Getter
    @Setter
    private String players;

    @Getter
    @Setter
    private boolean online;

    @Getter
    @Setter
    private Long lastOnline;

    @Getter
    @Setter
    private String city;

    @Getter
    @Setter
    private Double latitude;

    @Getter
    @Setter
    private Double longitude;
}
