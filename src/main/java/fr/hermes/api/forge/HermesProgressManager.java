package fr.hermes.api.forge;

public interface HermesProgressManager {
    HermesProgressBar push(String title, int steps);

    interface HermesProgressBar {
        void step(String title);

        void pop();

        int getStep();
    }
}
