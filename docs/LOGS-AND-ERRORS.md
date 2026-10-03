# Device error handling
## Device error events

The live small API returned `packedValues = JSDeviceError|{...}` on 2026-10-03. The adapter decodes the payload into `deviceError` while retaining the original event and raw payload. Invalid JSON cannot discard the rest of the journal. Modes 0/1/2/3 mean isolated fault / communication lost / recovery / brief failure. Recovery remains in history and has a dedicated filter, but is excluded from the problem filter.

Device names are highlighted in the journal and in an administrator-only notice above the diagram. The notice uses 24 hours of journal history; the journal summary uses the selected period. Only a dated start with no matching later recovery is listed. This is historical evidence, not a claim about the current physical state. Source, device and register distinguish intervals; missing timestamps and failed refreshes do not become a healthy status. Diagram symbol mapping is not guessed from device names. Existing journal permissions are unchanged.

The liquid recuperator pump command uses WorkState2 `0x0306`, bit 14 (`0x4000`) from the existing current register map. The obsolete `0x009F` is not polled. Missing data stays null, and the icon uses the pump command independently of the recuperator state. This does not establish physical pump operation; hardware acceptance is still required.
