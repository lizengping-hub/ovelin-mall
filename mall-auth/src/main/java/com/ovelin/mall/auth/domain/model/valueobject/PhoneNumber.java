package com.ovelin.mall.auth.domain.model.valueobject;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.ovelin.mall.common.exception.global.InvalidArgumentException;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * 非常宽松的格式，基本只要是真实的手机号，任何格式都可以读入</br>
 * 必须要是手机号
 */
public class PhoneNumber {

    private static final PhoneNumberUtil PHONE_UTIL = PhoneNumberUtil.getInstance();
    private static final Pattern SUPPORT_REGION_PATTERN = Pattern.compile("^[A-Z]{2}$"); // ISO 3166-1 alpha-2
    private static final Set<String> SUPPORTED_REGIONS = PHONE_UTIL.getSupportedRegions();
    private static final String DEFAULT_REGION = "CN"; // 默认地区码，可根据实际业务调整
    private final Phonenumber.PhoneNumber phoneNumber;


    // 显式声明为 private，外部无法再直接 new
    private PhoneNumber(Phonenumber.PhoneNumber phoneNumber) {
        // 这里仍可以做一层基础校验兜底，防止内部误用
        this.phoneNumber = phoneNumber;
    }

    public static PhoneNumber parseRawInput(String rawInput) {
        return parseRawInput(rawInput, DEFAULT_REGION);
    }
    public static PhoneNumber parseRawInput(String rawInput, String defaultRegion) {
        if (rawInput == null || rawInput.isBlank()) {
            throw new InvalidArgumentException("Phone number cannot be null or blank");
        }
        if (!SUPPORT_REGION_PATTERN.matcher(defaultRegion).matches()) {
            throw new InvalidArgumentException("Invalid default region: " + defaultRegion);
        }
        if (!SUPPORTED_REGIONS.contains(defaultRegion)) {
            throw new InvalidArgumentException("Unsupported default region: " + defaultRegion);
        }
        Phonenumber.PhoneNumber phoneNumber;
        try {
            phoneNumber = PHONE_UTIL.parse(rawInput.trim(), defaultRegion);
        } catch (NumberParseException e) {
            throw new InvalidArgumentException("Cannot parse phone number: " + rawInput + ": " + e.getMessage());
        }
        if (!PHONE_UTIL.isValidNumber(phoneNumber)) {
            throw new InvalidArgumentException("Invalid phone number: " + rawInput);
        }
        if (!PHONE_UTIL.getNumberType(phoneNumber).equals(PhoneNumberUtil.PhoneNumberType.MOBILE)){
            throw new InvalidArgumentException("Must be a mobile phone number: " + rawInput);
        }
        phoneNumber.setRawInput(rawInput);
        return new PhoneNumber(phoneNumber);
    }

    public String getRegion(){
        return PHONE_UTIL.getRegionCodeForNumber(phoneNumber);
    }
    public String getE164() {
        return PHONE_UTIL.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164);
    }
    public String getRawNumber(){
        return this.phoneNumber.getRawInput();
    }
    public String getNumber(){
        return PHONE_UTIL.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.NATIONAL);
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PhoneNumber other)) return false;
        return getE164().equals(other.getE164());
    }

    @Override
    public int hashCode() {
        return getE164().hashCode();
    }

    @Override
    public String toString() {
        return getE164();
    }

    public static Set<String> getSupportedRegions() {
        return SUPPORTED_REGIONS;
    }

}