package com.safesupport.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record ApplyRequest(
        @NotNull(message = "请选择生效日期")
        LocalDate effectiveDate,

        @Size(max = 200, message = "备注请控制在 200 字以内")
        String note,

        @NotEmpty(message = "请至少填写一位家属")
        @Valid
        List<ContactRequest> contacts
) {
    public record ContactRequest(
            @NotBlank(message = "请填写家属称呼")
            @Size(max = 20, message = "称呼过长")
            String name,

            @NotBlank(message = "请填写手机号")
            @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
            String phone,

            @Email(message = "邮箱格式不正确")
            @Size(max = 120, message = "邮箱过长")
            String email
    ) {
    }
}
