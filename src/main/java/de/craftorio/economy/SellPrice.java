package de.craftorio.economy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Credits paid per item at a trading post. Attached to items through the {@code craftorio:sell_prices} data map. */
public record SellPrice(long price) {
    public static final Codec<SellPrice> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.validate(price -> price > 0
                    ? DataResult.success(price)
                    : DataResult.error(() -> "price must be positive: " + price))
                    .fieldOf("price").forGetter(SellPrice::price)
    ).apply(instance, SellPrice::new));
}
