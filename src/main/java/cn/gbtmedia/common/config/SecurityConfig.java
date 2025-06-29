package cn.gbtmedia.common.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import java.util.Arrays;
import java.util.List;

/**
 * @author xqs
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "spring.security")
public class SecurityConfig {

    private List<String> ignorePaths = Arrays.asList("/zlmediakit/**","/backend/**");

    @Bean
    public PasswordEncoder passwordEncoder() {return new BCryptPasswordEncoder();}

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.info("SecurityConfig ignorePaths {}",ignorePaths);
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests (authorize -> authorize
                    .requestMatchers(ignorePaths.toArray(new String[0])).permitAll()
                    .anyRequest ().authenticated ()
                )
                .formLogin(Customizer.withDefaults())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}
