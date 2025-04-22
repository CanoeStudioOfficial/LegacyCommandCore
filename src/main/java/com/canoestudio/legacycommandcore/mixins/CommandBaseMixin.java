package com.canoestudio.legacycommandcore.mixins;

import com.google.common.base.Functions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import net.minecraft.command.CommandBase;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.Collection;
import java.util.List;

@Mixin(CommandBase.class)
public class CommandBaseMixin {
    /**
     * @author MQ-sing
     * @reason Add Array.sort call
     */
    @Overwrite
    public static List<String> getListOfStringsMatchingLastWord(String[] inputArgs, Collection<?> possibleCompletions){
        String s = inputArgs[inputArgs.length - 1];
        List<String> list = Lists.newArrayList();

        if (!possibleCompletions.isEmpty())
        {
            for (String s1 : Iterables.transform(possibleCompletions, Functions.toStringFunction()))
            {
                if (CommandBase.doesStringStartWith(s, s1))
                {
                    list.add(s1);
                }
            }

            if (list.isEmpty())
            {
                for (Object object : possibleCompletions)
                {
                    if (object instanceof ResourceLocation && CommandBase.doesStringStartWith(s, ((ResourceLocation)object).getPath()))
                    {
                        list.add(String.valueOf(object));
                    }
                }
            }
        }
        //Modify next line
        list.sort(String::compareTo);
        return list;
    }
}
