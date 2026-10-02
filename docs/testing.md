# Testing panel

Available only to ROLE_TESTER, including every /api/v1/testing endpoint. Both installation variants share the same implementation and retain their existing PLC/logName configuration.

Mode uses command 14 at 0x0300 and bit 3 (zero-based) of 0x0302, following the supplied drawing. POST /mode takes the desired boolean state; it reads the current state before toggling and returns the observed state. The UI reports an unconfirmed switch instead of presenting the requested state as confirmed.

TestingService serializes complete parameter → command → result operations across HTTP clients. Command writes use writeCommandOnce, avoiding replay after uncertain connection failures. Parameter writes reuse the existing register client. No commands run on page load or on section/device selection. Only mode status is polled every ten seconds. Results show their reading time and are cleared before a new read attempt.

Commands 1–4 have no parameters. Command 5 writes percentage × 100 at 0x0308. Command 6 reads the two float words at 0x030E/0x030F using the existing low-word-first decoder. Commands 7–11 select motor 0–3 via 0x0308. Command 7 writes whole percent at 0x0309; command 9 divides the integer result by 100. Command 11 displays the raw error mask without inventing bit meanings.

The drawings take priority over the conflicting spreadsheet: command 12 controls dampers (index 0–3 at 0x0308, close=0/open=1 at 0x0309); command 13 reads their position from 0x030C and displays percent as shown in the drawing. Command 14 controls valves Hot1=0/Cool1=1/Hot2=2/Cool2=3 with whole percent at 0x0309. Command 15 is unavailable because the spreadsheet marks it unimplemented.

PLC acceptance still requires confirming completion timing for result reads, mode-bit numbering, and damper result units. The supplied contract specifies reading results after command submission; no undocumented ready register or arbitrary delay is used. The drawings and spreadsheet do not provide the recuperator/pump error registers or mask meanings, so that diagnostic is explicitly unavailable. No live equipment settings were changed during development.

Deployment follows the existing push-to-main release and server updater workflow. Local tests and mock UI verification do not confirm installation or PLC behavior.
