package com.devsphere.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimiterConfigProperties {

    private boolean enabled = true;
    private boolean failOpen = true;
    private long timeoutMs = 2000;

    private Policy login = new Policy(5, 10);
    private Policy registration = new Policy(5, 10);
    private Policy authenticated = new Policy(20, 40);
    private Policy publicDefault = new Policy(10, 20);

    public static class Policy {
        private int replenishRate;
        private int burstCapacity;

        public Policy() {
        }

        public Policy(int replenishRate, int burstCapacity) {
            this.replenishRate = replenishRate;
            this.burstCapacity = burstCapacity;
        }

        public int getReplenishRate() {
            return replenishRate;
        }

        public void setReplenishRate(int replenishRate) {
            this.replenishRate = replenishRate;
        }

        public int getBurstCapacity() {
            return burstCapacity;
        }

        public void setBurstCapacity(int burstCapacity) {
            this.burstCapacity = burstCapacity;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isFailOpen() {
        return failOpen;
    }

    public void setFailOpen(boolean failOpen) {
        this.failOpen = failOpen;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public Policy getLogin() {
        return login;
    }

    public void setLogin(Policy login) {
        this.login = login;
    }

    public Policy getRegistration() {
        return registration;
    }

    public void setRegistration(Policy registration) {
        this.registration = registration;
    }

    public Policy getAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(Policy authenticated) {
        this.authenticated = authenticated;
    }

    public Policy getPublicDefault() {
        return publicDefault;
    }

    public void setPublicDefault(Policy publicDefault) {
        this.publicDefault = publicDefault;
    }
}
