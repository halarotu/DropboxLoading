package fi.ha.controller;

import com.dropbox.core.v2.files.FileMetadata;
import fi.ha.service.LoadingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AppController {

    @Autowired
    LoadingService loadingService;

    @RequestMapping(value = "/control", method = RequestMethod.GET)
    public String getImage(@RequestParam(name = "previous", required = false) Boolean previous, Model model) {
        FileMetadata fileMetadata;
        if (previous != null && previous == true) {
            fileMetadata = loadingService.getFileMetadata(true);
        }
        else {
            fileMetadata = loadingService.getFileMetadata(false);
        }

        if (fileMetadata != null) {
            model.addAttribute("image", fileMetadata.getName());
            model.addAttribute("date", fileMetadata.getMediaInfo().getMetadataValue().getTimeTaken());
            model.addAttribute("rotate", loadingService.rotateImage(fileMetadata));
        }
        model.addAttribute("autoupdate", !loadingService.isAutoUpdateStopped());
        model.addAttribute("stopButtonText", loadingService.isAutoUpdateStopped() ? "Jatka" : "Pysäytä tähän");

        return "main";
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String getImageV2(Model model) {

        FileMetadata fileMetadata = loadingService.getFileMetadata(false);
        if (fileMetadata != null) {
            model.addAttribute("image", fileMetadata.getName());
            model.addAttribute("rotate", loadingService.rotateImage(fileMetadata));
        }
        model.addAttribute("autoupdate", !loadingService.isAutoUpdateStopped());

        return "main2";
    }

    @RequestMapping(value = "/startStop", method = RequestMethod.POST)
    public String startStop() {
        if (loadingService.isAutoUpdateStopped()) {
            loadingService.setAutoUpdateStopped(false);
        } else {
            loadingService.setAutoUpdateStopped(true);
        }
        return "redirect:/control";
    }

    @RequestMapping(value = "/previous", method = RequestMethod.POST)
    public String getPrevious(RedirectAttributes redirectAttributes) {
        redirectAttributes.addAttribute("previous", true);
        return "redirect:/control";
    }

    @RequestMapping(value = "/stopped", method = RequestMethod.GET)
    @ResponseBody
    public boolean getStopped() {
        return loadingService.isAutoUpdateStopped();
    }
}
