package com.sky.controller.notify;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.service.OrderService;
import com.sky.properties.WeChatProperties;
import com.wechat.pay.contrib.apache.httpclient.util.AesUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.entity.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信支付回调接口
 */
@RestController
@RequestMapping("/notify")
@Slf4j
public class PayNotifyController {

    @Autowired
    private OrderService orderService;
    @Autowired
    private WeChatProperties weChatProperties;

    /**
     * 接收微信支付成功回调
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping("/paySuccess")
    public void paySuccessNotify(HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.info("收到微信支付成功回调");
        //读取微信支付回调报文
        String body = readData(request);
        //使用API V3密钥解密回调数据
        String plainText = decryptData(body);
        JSONObject jsonObject = JSON.parseObject(plainText);
        String outTradeNo = jsonObject.getString("out_trade_no");
        log.info("微信支付成功回调解密完成：orderNumber={}", outTradeNo);

        //修改订单支付状态并处理支付成功后的业务
        orderService.paySuccess(outTradeNo);

        //向微信支付平台返回成功响应，避免重复通知
        responseToWeixin(response);
        log.info("微信支付成功回调处理完成：orderNumber={}", outTradeNo);
    }

    /**
     * 读取微信支付回调请求体
     *
     * @param request
     * @return
     * @throws Exception
     */
    private String readData(HttpServletRequest request) throws Exception {
        BufferedReader reader = request.getReader();
        StringBuilder result = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            if (result.length() > 0) {
                result.append('\n');
            }
            result.append(line);
        }
        return result.toString();
    }

    /**
     * 解密微信支付回调数据
     *
     * @param body
     * @return
     * @throws Exception
     */
    private String decryptData(String body) throws Exception {
        JSONObject resultObject = JSON.parseObject(body);
        JSONObject resource = resultObject.getJSONObject("resource");
        String ciphertext = resource.getString("ciphertext");
        String nonce = resource.getString("nonce");
        String associatedData = resource.getString("associated_data");
        AesUtil aesUtil = new AesUtil(weChatProperties.getApiV3Key().getBytes(StandardCharsets.UTF_8));
        return aesUtil.decryptToString(associatedData.getBytes(StandardCharsets.UTF_8),
                nonce.getBytes(StandardCharsets.UTF_8), ciphertext);
    }

    /**
     * 向微信支付平台返回成功响应
     *
     * @param response
     * @throws Exception
     */
    private void responseToWeixin(HttpServletResponse response) throws Exception {
        response.setStatus(200);
        Map<String, String> result = new HashMap<>();
        result.put("code", "SUCCESS");
        result.put("message", "SUCCESS");
        response.setHeader("Content-Type", ContentType.APPLICATION_JSON.toString());
        response.getOutputStream().write(JSON.toJSONString(result).getBytes(StandardCharsets.UTF_8));
        response.flushBuffer();
    }
}
