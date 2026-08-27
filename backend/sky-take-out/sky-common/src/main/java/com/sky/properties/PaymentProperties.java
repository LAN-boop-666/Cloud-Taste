package com.sky.properties;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * 支付模式配置
 */
@Component
@ConfigurationProperties(prefix = "sky.payment")
@Data
@Slf4j
public class PaymentProperties {

    /**
     * WECHAT为真实微信支付，MOCK为本地模拟支付
     */
    private Mode mode = Mode.WECHAT;

    public enum Mode {
        WECHAT,
        MOCK
    }

    /**
     * 判断是否启用本地模拟支付
     *
     * @return
     */
    public boolean isMock() {
        return Mode.MOCK.equals(mode);
    }

    /**
     * 启动时输出当前支付模式
     */
    @PostConstruct
    public void logPaymentMode() {
        if (isMock()) {
            log.warn("\n==================================================\n" +
                    "当前支付模式：MOCK（本地模拟支付）\n" +
                    "不会调用微信支付和微信退款接口\n" +
                    "恢复真实支付：将 sky.payment.mode 改为 WECHAT\n" +
                    "==================================================");
            return;
        }
        log.info("\n==================================================\n" +
                "当前支付模式：WECHAT（真实微信支付）\n" +
                "请确认商户号、证书、密钥和回调地址均已配置\n" +
                "==================================================");
    }
}
