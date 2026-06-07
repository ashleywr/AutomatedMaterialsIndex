package com.sanhiruzu.ami.index.runtime;

import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.resolvers.PlayerResolver;
import com.sanhiruzu.ami.player.PlayerWaypointProviders;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public final class RuntimeSearchProviders {
    private static final CopyOnWriteArrayList<RuntimeSearchProvider> PROVIDERS = new CopyOnWriteArrayList<>();

    static {
        register(new RuntimeSearchProvider() {
            @Override
            public String id() {
                return "ami:online_players";
            }

            @Override
            public long revision() {
                return PlayerResolver.liveStateRevision();
            }

            @Override
            public List<SearchNode> nodes() {
                return PlayerResolver.livePlayerNodes();
            }
        });
        register(new RuntimeSearchProvider() {
            @Override
            public String id() {
                return "ami:waypoints";
            }

            @Override
            public long revision() {
                return PlayerWaypointProviders.liveWaypointRevision();
            }

            @Override
            public List<SearchNode> nodes() {
                return PlayerWaypointProviders.liveWaypointNodes();
            }
        });
    }

    private RuntimeSearchProviders() {
    }

    public static void register(RuntimeSearchProvider provider) {
        if (provider == null || provider.id() == null || provider.id().isBlank()) {
            return;
        }
        PROVIDERS.removeIf(existing -> Objects.equals(existing.id(), provider.id()));
        PROVIDERS.add(provider);
    }

    public static List<SearchNode> nodes() {
        List<SearchNode> out = new ArrayList<>();
        for (RuntimeSearchProvider provider : PROVIDERS) {
            try {
                out.addAll(provider.nodes());
            } catch (RuntimeException | LinkageError ignored) {
            }
        }
        out.sort(Comparator
                .comparing(SearchNode::type)
                .thenComparing(SearchNode::displayName, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(out);
    }

    public static long revision() {
        long revision = 17L;
        for (RuntimeSearchProvider provider : PROVIDERS) {
            try {
                revision = 31L * revision + provider.id().hashCode();
                revision = 31L * revision + provider.revision();
            } catch (RuntimeException | LinkageError ignored) {
            }
        }
        return revision;
    }
}
