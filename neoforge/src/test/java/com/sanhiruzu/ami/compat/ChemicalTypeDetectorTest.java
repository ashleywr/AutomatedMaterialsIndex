package com.sanhiruzu.ami.compat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ChemicalTypeDetectorTest {

    @Test
    void recognizesChemicalType() {
        assertTrue(ChemicalTypeDetector.isChemicalType("mekanism:ChemicalStack"));
        assertTrue(ChemicalTypeDetector.isChemicalType("net.minecraft.chemical.GasStack"));
    }

    @Test
    void recognizesGasType() {
        assertTrue(ChemicalTypeDetector.isChemicalType("mekanism:Gas"));
        assertTrue(ChemicalTypeDetector.isChemicalType("gas_type"));
    }

    @Test
    void recognizesPigmentType() {
        assertTrue(ChemicalTypeDetector.isChemicalType("mekanism:Pigment"));
        assertTrue(ChemicalTypeDetector.isChemicalType("pigment_handler"));
    }

    @Test
    void recognizesSlurryType() {
        assertTrue(ChemicalTypeDetector.isChemicalType("mekanism:Slurry"));
        assertTrue(ChemicalTypeDetector.isChemicalType("slurry_stack"));
    }

    @Test
    void recognizesInfuseType() {
        assertTrue(ChemicalTypeDetector.isChemicalType("mekanism:InfuseType"));
        assertTrue(ChemicalTypeDetector.isChemicalType("infuse_handler"));
    }

    @Test
    void caseInsensitive() {
        assertTrue(ChemicalTypeDetector.isChemicalType("CHEMICAL"));
        assertTrue(ChemicalTypeDetector.isChemicalType("GAS"));
        assertTrue(ChemicalTypeDetector.isChemicalType("Pigment"));
        assertTrue(ChemicalTypeDetector.isChemicalType("SLURRY"));
    }

    @Test
    void rejectsNonChemicalTypes() {
        assertFalse(ChemicalTypeDetector.isChemicalType("minecraft:item"));
        assertFalse(ChemicalTypeDetector.isChemicalType("fluid_bucket"));
        assertFalse(ChemicalTypeDetector.isChemicalType("potion"));
    }

    @Test
    void rejectsNull() {
        assertFalse(ChemicalTypeDetector.isChemicalType(null));
    }

    @Test
    void rejectsBlank() {
        assertFalse(ChemicalTypeDetector.isChemicalType(""));
        assertFalse(ChemicalTypeDetector.isChemicalType("   "));
    }
}
