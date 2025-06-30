package io.jmix.dependency.cli.dependency;

public enum SubscriptionPlan {

    BPM("bpm") {
        @Override
        public boolean isDependencyAllowed(MavenCoordinates mavenCoordinates) {
            return true;
        }
    },
    ENTERPRISE("enterprise") {
        @Override
        public boolean isDependencyAllowed(MavenCoordinates mavenCoordinates) {
            return !mavenCoordinates.getGroupId().equals("io.jmix.bpm");
        }
    };

    private final String id;

    SubscriptionPlan(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static SubscriptionPlan fromId(String id) {
        for (SubscriptionPlan plan : SubscriptionPlan.values()) {
            if (plan.getId().equalsIgnoreCase(id)) {
                return plan;
            }
        }
        return null;
    }

    public abstract boolean isDependencyAllowed(MavenCoordinates mavenCoordinates);
}
