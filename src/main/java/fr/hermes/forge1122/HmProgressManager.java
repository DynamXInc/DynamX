package fr.hermes.forge1122;

import fr.hermes.core.HermesProgressManager;
import lombok.RequiredArgsConstructor;
import net.minecraftforge.fml.common.ProgressManager;

public class HmProgressManager implements HermesProgressManager {
    @Override
    public HermesProgressBar push(String title, int steps) {
        return new HmProgressBar(ProgressManager.push(title, steps));
    }

    @RequiredArgsConstructor
    public static class HmProgressBar implements HermesProgressBar {
        private final ProgressManager.ProgressBar progressBar;

        @Override
        public void step(String title) {
            progressBar.step(title);
        }

        @Override
        public void pop() {
            ProgressManager.pop(progressBar);
        }
    }
}
