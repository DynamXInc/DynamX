package fr.dynamx.api.contentpack.object;

import fr.dynamx.api.contentpack.object.part.BasePart;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.utils.debug.DynamXDebugOption;
import scala.xml.dtd.impl.Base;

import java.util.List;

public interface IShapeProvider<T extends ISubInfoTypeOwner<T>> {

    <C extends BasePart<T>> List<C> getAllParts();

}
