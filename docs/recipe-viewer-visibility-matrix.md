# Recipe Viewer Visibility Matrix

This table is the intended screen-level contract for AMI, EMI/JEI, the vanilla recipe book button, and the Alt-V keybind.

| State / action | AMI panel | EMI/JEI chrome | Notes |
| --- | --- | --- | --- |
| Inventory opens, Start AMI Hidden off | Visible | Suppressed if loaded | Default AMI-first behavior. |
| Inventory opens, Start AMI Hidden on | Hidden | Visible if loaded | The config only chooses AMI's initial state. |
| Press Alt-V while AMI is visible | Hidden | Visible if loaded | Alt-V must always switch away from AMI. |
| Press Alt-V while AMI is hidden | Visible | Suppressed if loaded | Alt-V must always switch back to AMI. |
| Click vanilla recipe book button while AMI is visible | Hidden | Suppressed if loaded | The recipe book button hides all recipe UI on supported container screens. |
| Click vanilla recipe book button while recipe-book hidden | Visible | Suppressed if loaded | Clicking again restores AMI. |
| Click vanilla recipe book button while AMI is hidden by Alt-V or Start AMI Hidden | Visible | Suppressed if loaded | The recipe book button switches back to AMI from the external-viewer state. |
| AMI hidden and no EMI/JEI loaded | Hidden | Not applicable | No AMI overlay is drawn. |
| Creative mode | No visibility effect | No visibility effect | Creative mode affects cheat/item access behavior only. |
| Survival mode | No visibility effect | No visibility effect | Survival mode does not decide which overlay owns the screen. |
| Show Hidden Mod Items on/off | No visibility effect | No visibility effect | Filters AMI result contents only. |
| Strict Survival Mode on/off | No visibility effect | No visibility effect | Filters/indexes AMI result contents only. |
| Recipe Viewer Mode: Auto/Native/EMI-JEI | No visibility effect | No visibility effect | Selects recipe lookup target, not overlay ownership. |

Regression coverage lives in `RecipeViewerSuppressionPolicyTest.screenVisibilityMatrixStaysConsistent`.
