package cn.maian.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.bootstrap-admin")
public record BootstrapAdminProperties(String phone, String password, String nickname) {
    public boolean requested() {
        return (phone != null && !phone.isBlank()) || (password != null && !password.isBlank());
    }

    public boolean configured() {
        return phone != null && !phone.isBlank() && password != null && !password.isBlank();
    }
}
