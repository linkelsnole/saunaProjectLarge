import { useState, useLayoutEffect } from "react";
import { createPortal } from "react-dom";
import { useRecuperatorType } from "@/hooks/useRecuperatorType";
import { useSettingsPanelBounds } from "@/contexts/SettingsPanelBoundsContext";
import { Button } from "@/shared/ui/button";
import { GlassCard } from "@/shared/ui/GlassCard";
import { Input } from "@/shared/ui/input";
import { Loader2, CheckCircle2, XCircle } from "lucide-react";
import { toast } from "sonner";

const defaultRegisters = [
	{ id: 1, address: "0x00A4", name: "Рекуператор Статус", multiplier: "1" },
	{ id: 2, address: "0x0081", name: "Целевые обороты", multiplier: "100" },
	{ id: 3, address: "0x0080", name: "Текущие обороты", multiplier: "100" },
	{ id: 4, address: "", name: "Вентилятор Приток", multiplier: "1" },
	{ id: 5, address: "", name: "Вентилятор Вытяжка", multiplier: "1" },
];

export const ConfiguratorWidget = () => {
	const { type, changeType } = useRecuperatorType();
	const [isModalOpen, setIsModalOpen] = useState(false);

	const [draftType, setDraftType] = useState<1 | 2>(type);

	const [registers, setRegisters] = useState(defaultRegisters);

	const [validation, setValidation] = useState<Record<number, "success" | "error" | "idle">>({});
	const [isValidating, setIsValidating] = useState(false);

	const { boundsRef } = useSettingsPanelBounds();
	const [portalContainer, setPortalContainer] = useState<HTMLElement | null>(null);

	useLayoutEffect(() => {
		if (isModalOpen) setPortalContainer(boundsRef.current);
		else setPortalContainer(null);
	}, [isModalOpen, boundsRef]);

	const handleTypeSelect = (selectedType: 1 | 2) => {
		setDraftType(selectedType);
		setValidation({});
		setIsModalOpen(true);
	};

	const handleCancel = () => {
		setIsModalOpen(false);
		setDraftType(type);
		setValidation({});
	};

	const handleSave = () => {
		setIsValidating(true);

		setTimeout(() => {
			let hasErrors = false;
			const newValidation: Record<number, "success" | "error" | "idle"> = {};

			registers.forEach((reg) => {
				if (!reg.address.trim()) {
					newValidation[reg.id] = "idle";
					return;
				}

				if (reg.address.toLowerCase() === "0x9999") {
					newValidation[reg.id] = "error";
					hasErrors = true;
				} else {
					newValidation[reg.id] = "success";
				}
			});

			setValidation(newValidation);
			setIsValidating(false);

			if (hasErrors) {
				toast.error("Ошибка чтения некоторых регистров. Проверьте адреса.");
			} else {
				toast.success("Регистры успешно прочитаны. Настройки применены.");
				changeType(draftType);
				setIsModalOpen(false);
			}
		}, 1000);
	};

	const updateRegister = (id: number, field: "address" | "multiplier", value: string) => {
		setRegisters((prev) => prev.map((r) => (r.id === id ? { ...r, [field]: value } : r)));
		if (validation[id] === "error") {
			setValidation((prev) => ({ ...prev, [id]: "idle" }));
		}
	};

	const modal =
		isModalOpen &&
		portalContainer &&
		createPortal(
			<>
				<div
					className="absolute inset-0 z-[80] rounded-[inherit] bg-slate-900/40 backdrop-blur-sm"
					aria-hidden
				/>
				<div className="absolute inset-0 z-[90] flex items-center justify-center overflow-hidden p-4 animate-in fade-in duration-200 pointer-events-none">
					<div
						className="pointer-events-auto bg-white rounded-2xl shadow-2xl w-full max-w-2xl overflow-hidden border border-slate-200 flex flex-col max-h-[min(90vh,100%)]"
						onClick={(e) => e.stopPropagation()}
					>
						<div className="p-5 border-b border-slate-100 bg-slate-50/80 flex justify-between items-center shrink-0">
							<h3 className="font-bold text-slate-700 text-lg">Окно настроек (Тип {draftType})</h3>
						</div>

						<div className="p-6 space-y-4 overflow-y-auto flex-1 custom-scrollbar">
							<div className="flex gap-4 text-xs font-bold text-slate-500 uppercase tracking-wide px-1">
								<div className="w-[120px] shrink-0">Адрес</div>
								<div className="w-[80px] shrink-0">Множ.</div>
								<div className="flex-1">Наименование элемента</div>
								<div className="w-[24px] shrink-0"></div>
							</div>

							<div className="space-y-3">
								{registers.map((reg) => {
									const status = validation[reg.id] || "idle";
									return (
										<div key={reg.id} className="flex gap-4 items-center">
											<Input
												placeholder="0x..."
												className={`w-[120px] font-mono text-sm shrink-0 transition-colors ${
													status === "error"
														? "border-red-500 bg-red-50 text-red-700 focus-visible:ring-red-500"
														: status === "success"
															? "border-emerald-500 bg-emerald-50 text-emerald-700 focus-visible:ring-emerald-500"
															: ""
												}`}
												value={reg.address}
												onChange={(e) => updateRegister(reg.id, "address", e.target.value)}
												disabled={isValidating}
											/>

											<Input
												placeholder="1"
												type="number"
												className="w-[80px] text-sm shrink-0 text-center"
												value={reg.multiplier}
												onChange={(e) => updateRegister(reg.id, "multiplier", e.target.value)}
												disabled={isValidating}
											/>

											<div className="text-slate-300">↔</div>

											<Input
												className="flex-1 text-sm bg-slate-50/50 cursor-default"
												value={reg.name}
												readOnly
											/>

											<div className="w-[24px] shrink-0 flex justify-center">
												{status === "success" && <CheckCircle2 className="w-5 h-5 text-emerald-500" />}
												{status === "error" && <XCircle className="w-5 h-5 text-red-500" />}
											</div>
										</div>
									);
								})}
							</div>

							<div className="mt-6 p-3 bg-blue-50/50 rounded-lg border border-blue-100 text-xs text-blue-600">
								<span className="font-bold">Подсказка:</span> Введите адрес{" "}
								<code className="font-mono bg-blue-100 px-1 rounded">0x9999</code>, чтобы сымитировать
								ошибку чтения (регистр покраснеет).
							</div>
						</div>

						<div className="p-4 border-t border-slate-100 bg-slate-50 flex justify-end gap-3 shrink-0">
							<Button variant="outline" onClick={handleCancel} disabled={isValidating}>
								Отмена
							</Button>
							<Button
								onClick={handleSave}
								disabled={isValidating}
								className="w-32 bg-blue-600 hover:bg-blue-700"
							>
								{isValidating ? <Loader2 className="w-4 h-4 animate-spin" /> : "Ввод"}
							</Button>
						</div>
					</div>
				</div>
			</>,
			portalContainer,
		);

	return (
		<>
			<div className="space-y-6 pb-6">
				<GlassCard className="p-4">
					<h4 className="text-sm font-bold text-slate-700 tracking-wider mb-4 flex items-center gap-2">
						<div className="w-2 h-2 bg-purple-500 rounded-full shadow-[0_0_8px_rgba(168,85,247,0.8)]" />
						Схема рекуператора
					</h4>
					<p className="text-xs text-slate-500 mb-4">
						Выберите тип схемы. Текущий активный: <strong>Тип {type}</strong>
					</p>

					<div className="grid grid-cols-2 gap-4">
						<div
							onClick={() => handleTypeSelect(1)}
							className={`cursor-pointer border-2 rounded-xl p-4 flex flex-col items-center justify-center transition-all ${
								type === 1
									? "border-blue-500 bg-blue-50/50 shadow-sm"
									: "border-slate-200 hover:border-blue-300 hover:bg-slate-50"
							}`}
						>
							<div className="w-full h-24 bg-slate-100 rounded-md mb-3 flex items-center justify-center border border-dashed border-slate-300 text-slate-400 text-xs">
								[Место под картинку Тип 1]
							</div>
							<span className={`font-bold ${type === 1 ? "text-blue-600" : "text-slate-600"}`}>
								Тип 1 (Полный)
							</span>
						</div>

						<div
							onClick={() => handleTypeSelect(2)}
							className={`cursor-pointer border-2 rounded-xl p-4 flex flex-col items-center justify-center transition-all ${
								type === 2
									? "border-blue-500 bg-blue-50/50 shadow-sm"
									: "border-slate-200 hover:border-blue-300 hover:bg-slate-50"
							}`}
						>
							<div className="w-full h-24 bg-slate-100 rounded-md mb-3 flex items-center justify-center border border-dashed border-slate-300 text-slate-400 text-xs">
								[Место под картинку Тип 2]
							</div>
							<span className={`font-bold ${type === 2 ? "text-blue-600" : "text-slate-600"}`}>
								Тип 2 (Упрощенный)
							</span>
						</div>
					</div>
				</GlassCard>
			</div>
			{modal}
		</>
	);
};
