/**
 * A functional interface for handling back button click events.
 * This interface defines a single abstract method, making it suitable for use 
 * with lambda expressions or method references.
 */
@FunctionalInterface
public interface BackButtonHandler {
    /**
     * Executes the action to handle a back button click event.
     */
    void onBackButtonClick();
}
