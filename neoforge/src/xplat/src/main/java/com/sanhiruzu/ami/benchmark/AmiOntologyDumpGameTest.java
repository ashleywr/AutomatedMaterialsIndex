package com.sanhiruzu.ami.benchmark;

import com.sanhiruzu.ami.neoforge.AMI;
import com.sanhiruzu.ami.index.AmiIndexerService;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.nio.file.Path;

@PrefixGameTestTemplate(false)
public class AmiOntologyDumpGameTest {

    @GameTest(templateNamespace = AMI.MODID, template = "ami_benchmark_empty", setupTicks = 1L, timeoutTicks = 600)
    public static void dumpOntology(GameTestHelper helper) {
        boolean ontologyDumpMode = "true".equals(System.getProperty("ami.ontology_dump_mode"));
        boolean fallbackDumpMode = "true".equals(System.getProperty("ami.fallback_dump_mode"));
        boolean materialDumpMode = "true".equals(System.getProperty("ami.material_dump_mode"));
        if (!ontologyDumpMode && !fallbackDumpMode && !materialDumpMode) {
            helper.succeed();
            return;
        }

        try {
            AMI.LOGGER.info("Starting headless ontology/material dump...");
            AmiIndexerService indexer = AmiIndexerService.getInstance();
            indexer.rebuild(helper.getLevel());
            while (!indexer.isReady()) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {
                }
            }

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
            helper.succeed();
        } catch (Exception e) {
            AMI.LOGGER.error("Failed to dump ontology headlessly", e);
            helper.fail(e.getMessage());
        }
    }
}
