package com.ovelin.mall.common.web.starter.jackson;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;



@RestController
public class LongToStringCustomizerController {
    @GetMapping("/echo")
    IdBody get() {
        return new IdBody(9007199254740993L, List.of(9007199254740993L), 7);
    }

    @PostMapping("/echo")
    IdBody post(@RequestBody IdBody body) {
        return body;
    }
}
