package com.example.cinemaster.controller;

import com.example.cinemaster.dto.request.AccountDTO;
import com.example.cinemaster.dto.request.AccountUpdateDTO;
import com.example.cinemaster.dto.request.PasswordUpdateDTO;
import com.example.cinemaster.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    @Autowired
    private AccountService accountService;

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName(); // Returns the email (username)
    }

    @GetMapping
    public String showProfile(Model model) {
        String email = getCurrentUserEmail();
        try {
            AccountDTO account = accountService.getAccountProfile(email);
            model.addAttribute("account", account);
            model.addAttribute("accountUpdateDTO", new AccountUpdateDTO(account.getFullName(), account.getPhoneNumber(), account.getEmail(), account.getAddress()));
            model.addAttribute("passwordUpdateDTO", new PasswordUpdateDTO());
            return "profile";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error loading profile: " + e.getMessage());
            return "error"; // Or redirect to a generic error page
        }
    }

    @PostMapping("/update")
    public String updateProfile(@Valid @ModelAttribute("accountUpdateDTO") AccountUpdateDTO accountUpdateDTO,
                                BindingResult result,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        String email = getCurrentUserEmail();
        if (result.hasErrors()) {
            AccountDTO currentAccount = accountService.getAccountProfile(email);
            model.addAttribute("account", currentAccount);
            model.addAttribute("passwordUpdateDTO", new PasswordUpdateDTO());
            return "profile";
        }
        try {
            accountService.updateAccountProfile(email, accountUpdateDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating profile: " + e.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/change-password")
    public String changePassword(@Valid @ModelAttribute("passwordUpdateDTO") PasswordUpdateDTO passwordUpdateDTO,
                                 BindingResult result,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        String email = getCurrentUserEmail();
        if (result.hasErrors()) {
            AccountDTO currentAccount = accountService.getAccountProfile(email);
            model.addAttribute("account", currentAccount);
            model.addAttribute("accountUpdateDTO", new AccountUpdateDTO(currentAccount.getFullName(), currentAccount.getPhoneNumber(), currentAccount.getEmail(), currentAccount.getAddress()));
            return "profile";
        }
        try {
            accountService.updateAccountPassword(email, passwordUpdateDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Password updated successfully!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error changing password: " + e.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/upload-avatar")
    public String uploadAvatar(@RequestParam("avatarFile") MultipartFile file,
                               RedirectAttributes redirectAttributes) {
        String email = getCurrentUserEmail();
        try {
            AccountDTO account = accountService.getAccountProfile(email);
            accountService.saveAvatar(file, account.getAccountID());
            redirectAttributes.addFlashAttribute("successMessage", "Avatar updated successfully!");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to upload avatar: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/profile";
    }
}