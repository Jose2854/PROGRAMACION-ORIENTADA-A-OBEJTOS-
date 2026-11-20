package com.crm.controller;

import com.crm.dto.ZeuzImportResult;
import com.crm.service.ZeuzExcelImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/imports/zeuz")
@RequiredArgsConstructor
public class ZeuzImportController {

    private final ZeuzExcelImportService importService;

    @PostMapping(value = "/excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ZeuzImportResult importExcel(@RequestPart("file") MultipartFile file) {
        return importService.importFile(file);
    }
}
