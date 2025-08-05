package com.moneyflow.moneyflow.config;

import com.moneyflow.moneyflow.util.NumberFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ControllerAdvice
public class ThymeleafConfig {

    private static final Logger logger = LoggerFactory.getLogger(ThymeleafConfig.class);
    private final NumberFormatter numberFormatter;

    @Autowired
    public ThymeleafConfig(NumberFormatter numberFormatter) {
        this.numberFormatter = numberFormatter;
    }

    @PostConstruct
    public void init() {
        logger.info("ThymeleafConfig initialized with NumberFormatter: {}", numberFormatter);
    }

    @ModelAttribute("numberFormatter")
    public NumberFormatter addNumberFormatterToModel() {
        return numberFormatter;
    }
}