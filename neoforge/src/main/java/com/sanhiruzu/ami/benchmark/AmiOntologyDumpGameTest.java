package com.sanhiruzu.ami.benchmark;

import com.sanhiruzu.ami.index.AmiIndexerService;
import com.sanhiruzu.ami.neoforge.AMI;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;

public class AmiOntologyDumpGameTest {

    public static void dumpOntology(Runnable onSucceed, java.util.function.Consumer<String> onFail) {
        boolean ontologyDumpMode = "true".equals(System.getProperty("ami.ontology_dump_mode"));
        boolean fallbackDumpMode = "true".equals(System.getProperty("ami.fallback_dump_mode"));
        boolean materialDumpMode = "true".equals(System.getProperty("ami.material_dump_mode"));
        if (!ontologyDumpMode && !fallbackDumpMode && !materialDumpMode) {
            onSucceed.run();
            return;
        }

        try {
            AMI.LOGGER.info("Starting headless ontology/material dump...");
            runDump(ontologyDumpMode, fallbackDumpMode, materialDumpMode);
            onSucceed.run();
        } catch (Exception e) {
            AMI.LOGGER.error("Failed to dump ontology headlessly", e);
            onFail.accept(e.getMessage());
        }
    }

    private static void runDump(boolean ontologyDumpMode, boolean fallbackDumpMode, boolean materialDumpMode) throws Exception {
        Path configDir = FMLPaths.GAMEDIR.get().resolve("ami_dumps");
        if (ontologyDumpMode) {
            Path csvFile = configDir.resolve("ontology_dump.csv");
            AmiOntologyDiagnostics.exportOntologyCsv(csvFile);
            AMI.LOGGER.info("Headless AMI ontology exported to {}", csvFile.toAbsolutePath());
        }
        if (materialDumpMode) {
            Path csvFile = configDir.resolve("material_audit.csv");
            AmiOntologyDiagnostics.exportGroupingAuditCsv(csvFile);
            AMI.LOGGER.info("Headless AMI material audit exported to {}", csvFile.toAbsolutePath());
        }
        if (fallbackDumpMode) {
            Path csvFile = configDir.resolve("facet_fallback_sample.csv");
            AmiOntologyDiagnostics.FallbackSampleSummary summary = AmiOntologyDiagnostics.exportFacetFallbackCsv(csvFile);
            AMI.LOGGER.info(
                    "Headless AMI facet fallback sample exported to {} (all facetless: {}/{}, all unresolved facetful: {}, player-visible facetless: {}/{}, player-visible unresolved facetful: {}, visible legacy buckets: {})",
                    csvFile.toAbsolutePath(),
                    summary.facetlessItems(),
                    summary.totalItems(),
                    summary.unresolvedFacetfulItems(),
                    summary.playerVisibleFacetlessItems(),
                    summary.playerVisibleItems(),
                    summary.playerVisibleUnresolvedFacetfulItems(),
                    summary.playerVisibleLegacyCategoryCounts()
            );
        }
    }
}
