package analyzer;

public class PatternRecord {
    private final int priority;
    private final String pattern;
    private final String description;

    public PatternRecord(int priority, String pattern, String description) {
        this.priority = priority;
        this.pattern = pattern;
        this.description = description;
    }

    public int getPriority() {
        return priority;
    }

    public String getPattern() {
        return pattern;
    }

    public String getDescription() {
        return description;
    }
}
