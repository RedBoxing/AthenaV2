package fr.redboxing.athena.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@ToString
public class Tuple<A, B> {
    @Getter
    @Setter
    private A first;

    @Getter
    @Setter
    private B second;
}
